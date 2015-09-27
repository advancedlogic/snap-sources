package org.al.snap.sources.engine

import com.google.common.net.InternetDomainName
import com.gravity.goose.Configuration
import com.gravity.goose.Goose
import de.l3s.boilerpipe.extractors.ArticleExtractor
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.sax.LinkContentHandler
import org.apache.tika.sax.TeeContentHandler
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.util.*
import java.util.regex.Pattern

/**
 * Created by skywalker on 9/17/15.
 */

public object Crawler: Log, Utils {
    val config = Configuration()
    var initialized = false
    val MAX_PAGE_LEN = Conf.getOrDefault("crawler.max-page-len", 5000000)

    fun init() {
        if (!initialized) {
            var tmpPath = Conf.getOrDefault("crawler.path.tmp", "./tmp")
            var convertPath = Conf.getOrDefault("crawler.path.convert", "/usr/bin/convert")
            var identifyPath = Conf.getOrDefault("crawler.path.identify", "/usr/bin/identify")
            var minImageSize = Conf.getOrDefault("crawler.min-image-size", 10000)
            var imageConnectionTimeout = time2ms(Conf.getOrDefault("crawler.image-connection-timeout", "1s")).toInt()
            var imageSOTimeout = time2ms(Conf.getOrDefault("crawler.image-so-timeout", "5s")).toInt()
            config.setLocalStoragePath(tmpPath)
            config.setImagemagickConvertPath(convertPath)
            config.setImagemagickIdentifyPath(identifyPath)
            config.setMinBytesForImages(minImageSize)
            config.setImageConnectionTimeout(imageConnectionTimeout)
            config.setImageSocketTimeout(imageSOTimeout)
            initialized = true
        }
    }

    fun process(url:String): WebEntity {
        val entity = WebEntity()
        entity.url = url
        process(entity)
        return entity
    }

    fun process(entity: WebEntity) {
        try {
            tryToResolve(entity)
        } finally {
            tryToFetch(entity)
            tryToGuessDomain(entity)
            tryToGetLinks(entity)
            tryToGuessTopImage(entity)
            tryToCleanWithGoose(entity)
            tryToCleanWithBoilerPipe(entity)
        }
    }

    //Try to expand a shortened URL
    private fun tryToResolve(entity: WebEntity) {
        val url = entity.url
        val config = RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(if (url.length() < 30) 5000 else 1000) //this is for short urls
                .build()
        val httpHead = HttpHead(url)
        val context = HttpClientContext.create()
        val httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()
        var finalUrl = url
        httpClient.use {
            try {
                val response = httpClient.execute(httpHead, context)
                response.use {
                    val locations:Iterable<URI>? = context.getRedirectLocations()
                    if (locations != null) {
                        finalUrl = locations.last().toString()
                    }
                }
            } catch (e: IOException) {
                //warn(e.getMessage()!!)
            }
        }

        if (finalUrl != "") entity.finalUrl = finalUrl
        else entity.finalUrl = url
    }

    private fun tryToGetMIME(ist: InputStream):String {
        val tika = Tika()
        val mimeType = tika.detect(ist)
        return mimeType
    }

    private class HttpTimerTask(val httpClient:CloseableHttpClient):TimerTask(), Log {
        override fun run() {
            warn("Session timeout")
            httpClient.close()
        }
    }
    //Try to download the HTML
    private fun tryToFetch(entity: WebEntity) {
        try {
            val config = RequestConfig.custom()
                    .setSocketTimeout(entity.timeout.toInt())
                    .setConnectTimeout(entity.timeout.toInt())
                    .setConnectionRequestTimeout(entity.timeout.toInt())
                    .build()
            val httpGet = HttpGet(entity.finalUrl)
            val context = HttpClientContext.create()
            val httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()

            httpClient.use {
                try {
                    val timer = Timer()
                    timer.schedule(HttpTimerTask(httpClient), 5000)
                    val response = httpClient.execute(httpGet, context)
                    response.use {
                        timer.cancel()
                        val ist = response.getEntity().getContent()
                        ist.use {
                            val baos = ByteArrayOutputStream()
                            val buffer = ByteArray(1024)
                            var len = 0
                            while (len > -1) {
                                len = ist.read(buffer)
                                if (len > 0) baos.write(buffer, 0, len)
                                break
                            }
                            baos.flush()
                            val is1 = ByteArrayInputStream(baos.toByteArray())
                            val mimeType = tryToGetMIME(is1)

                            if (!mimeType.contains("audio") && !mimeType.contains("video")) {
                                var bytesCounter = 0
                                while (len > -1) {
                                    len = ist.read(buffer)
                                    bytesCounter += len
                                    if (bytesCounter > MAX_PAGE_LEN) {
                                        warn("Fetched $bytesCounter bytes for ${entity.finalUrl}")
                                        break
                                    }
                                    if (len > 0) baos.write(buffer, 0, len)
                                }
                                baos.flush()

                                if (!mimeType.contains("html")) {
                                    val parser = AutoDetectParser()
                                    val metadata = Metadata()
                                    val bodyHandler = BodyContentHandler(Integer.MAX_VALUE)
                                    val linkHandler = LinkContentHandler()
                                    val handler = TeeContentHandler(bodyHandler, linkHandler)
                                    parser.parse(ist, handler, metadata, ParseContext())
                                    entity.content = if(bodyHandler != null) bodyHandler.toString() else ""
                                    entity.title = metadata.get(TikaCoreProperties.TITLE)?:""
                                } else {
                                    val html = baos.toString()
                                    entity.document = Jsoup.parse(html)
                                    entity.html = html
                                }
                            } else {
                                entity.valid = false
                            }
                        }
                    }
                } catch(e: IOException) {
                }
            }

        } catch(e:Exception) {
            entity.valid = false
        }
    }

    fun tryToGuessDomain(entity: WebEntity) {
        try {
            val url = entity.finalUrl
            val fullDomainName = InternetDomainName.from(url)
            val publicDomainName = fullDomainName.topPrivateDomain()
            var topDomain = ""
            val it = publicDomainName.parts().iterator()
            while (it.hasNext()) {
                val part = it.next()
                if (!topDomain.isEmpty()) topDomain += "."
                topDomain += part
            }

            entity.domain = topDomain
        } catch(e:Exception) {}
    }

    fun tryToGetLinks(entity: WebEntity) {
        val document = entity.document
        val domain = entity.domain
        val levels = entity.levels
        val numbers = entity.numbers
        if (levels != 0 && numbers != 0 && document != null) {
            var elements = document.select("a[href]")
            try {
                for (element in elements?.iterator()) {
                    try {
                        val link = element.attr("abs:href")
                        if (link != null && domain in link) {
                            entity.links.add(link)
                        }
                    } catch(e: MalformedURLException) {
                        warn(e.getMessage() ?: "Error extracting href")
                    }
                }
            } catch(e:IllegalArgumentException) {
                warn(e.getMessage() ?: "Error extracting hrefs")
            }
            elements = document.select("link[href]")
            try {
                for (element in elements?.iterator()) {
                    try {
                        val link = element.attr("abs:href")
                        if (link != null && domain in link) {
                            entity.links.add(link)
                        }
                    } catch(e: MalformedURLException) {
                        warn(e.getMessage()?:"Error extracting href")
                    }
                }
            } catch(e:IllegalArgumentException) {
                warn(e.getMessage() ?: "Error extracting hrefs")
            }
        }
    }


    fun tryToCleanWithGoose(entity: WebEntity) {
        if (entity.content == "") {
            //Try to extract Top Image using Goose if not present
            this.config.setEnableImageFetching(entity.image == "")

            val url = entity.finalUrl
            val html = entity.html
            val goose = Goose(this.config)
            try {
                var article = goose.extractContent(url, html)
                if (article != null) {
                    entity.title = if (entity.title == "") article.getTitle()!! else entity.title
                    entity.keywords = article.getMetaKeywords()
                    entity.description = if (entity.description == "") article.getMetaDescription()!! else entity.description
                    entity.content = article.getCleanedArticleText()!!
                    entity.domain = if (entity.domain == "") article.getDomain()!! else entity.domain
                    if (entity.image == "") {
                        entity.image = article.topImage()?.getImageSrc()!!
                    }
                    debug("[Goose] Content: ${entity.content}")
                }
            } catch(t:Throwable) {
            }
        }
    }

    fun tryToCleanWithBoilerPipe(entity: WebEntity) {
        if (entity.content == "") {
            try {
                entity.content = ArticleExtractor.getInstance()?.getText(entity.html)!!
                debug("[Boilerpipe] Content: ${entity.content}")
            } catch (t:Throwable) {

            }
        }
    }

    fun tryToGuessTopImage(entity: WebEntity) {
        if (entity.document != null) {
            var topImage = openGraphResolver(entity.document!!)
            if (topImage == "") {
                topImage = webPageResolver(entity.document!!)
            }
            entity.image = topImage
            debug("[] Image: ${entity.image}")
        }
    }

    data class Candidate(val url:String, val surface:Int, val score:Int)

    val rules = hashMapOf(
            Pair(Pattern.compile("(large|big)"), 1),
            Pair(Pattern.compile("upload"), 1),
            Pair(Pattern.compile("media"), 1),
            Pair(Pattern.compile("gravatar.com"), -1),
            Pair(Pattern.compile("feeds.feedburner.com"), -1),
            Pair(Pattern.compile("(?i)icon"), -1),
            Pair(Pattern.compile("(?i)logo"), -1),
            Pair(Pattern.compile("(?i)spinner"), -1),
            Pair(Pattern.compile("(?i)loading"), -1),
            Pair(Pattern.compile("(?i)ads"), -1),
            Pair(Pattern.compile("badge"), -1),
            Pair(Pattern.compile("1x1"), -1),
            Pair(Pattern.compile("pixel"), -1),
            Pair(Pattern.compile("thumbnail[s]"), -1),
            Pair(Pattern.compile("(?i)icon"), -1),
            Pair(Pattern.compile(".html|.gif|.ico|button|twitter.jpg|facebook.jpg|ap_buy_photo|digg.jpg|digg.png|delicious.png|facebook.png|reddit.jpg|doubleclick|diggthis|diggThis|adserver|/ads/|ec.atdmt.com|mediaplex.com|adsatt|view.atdmt"), -1))

    private fun score(tag: Element):Int {
        var src = tag.attr("src")
        if (src == "") {
            src = tag.attr("data-src")
        }
        if (src == "") {
            src = tag.attr("data-lazy-src")
        }
        if (src == "") return -1

        var tagScore = 0
        for ((rule,score) in rules) {
            val matcher = rule.matcher(src!!)
            if (matcher.find()) {
                tagScore += score
            }
        }

        var alt = tag.attr("alt")
        if (alt != null && alt != "") {
            tagScore--
        }

        return tagScore
    }

    private fun webPageResolver(document: Document):String {
        val images = document.getElementsByTag("img")
        var topImage:String
        val candidates = HashSet<Candidate>()
        val significantSurface = 320 * 200
        var significantSurfaceCount = 0
        var src:String
        images?.forEach {
            var surface = 0
            src = it.attr("src").toString()
            if (src == "") {
                src = it.attr("data-src").toString()
            }
            if (src == "") {
                src = it.attr("data-lazy-src").toString()
            }
            if (src != "") {
                val width = it.attr("width").toString()
                val height = it.attr("height").toString()
                if (width != "" && !width.endsWith("%") && width != "auto") {
                    val w = width.replace("px","").replace(";", "").trim().toInt()
                    if (height != "" && !height.endsWith("%") && height != "auto") {
                        val h = height.replace("px","").replace(";", "").trim().toInt()
                        surface = w * h
                    } else {
                        surface = w
                    }
                } else {
                    if (height != "" && !height.endsWith("%") && height != "auto") {
                        surface = height.replace("px","").toInt()
                    } else {
                        surface = 0
                    }
                }
            }
            if (surface > significantSurface) {
                significantSurfaceCount++
            }

            val tagscore = score(it)
            if (tagscore >= 0) {
                val candidate = Candidate(src, surface, tagscore)
                candidates.add(candidate)
            }
        }

        if (candidates.size() == 0) return ""

        if (significantSurfaceCount > 0) {
            val bestCandidate = findBestCandidateFromSurface(candidates)
            topImage = bestCandidate.url
        } else {
            val bestCandidate = findBestCandidateFromScore(candidates)
            topImage = bestCandidate.url
        }

        if (topImage != "" && !topImage.startsWith("http")) {
            topImage = "http://" + topImage
        }

        return topImage
    }

    private fun findBestCandidateFromSurface(candidates:Set<Candidate>):Candidate {
        var max = 0
        var bestCandidate = Candidate("", -1, -1)
        for (candidate in candidates) {
            val surface = candidate.surface
            if (surface >= max) {
                max = surface
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    private fun findBestCandidateFromScore(candidates:Set<Candidate>):Candidate {
        var max = 0
        var bestCandidate = Candidate("", -1, -1)
        for (candidate in candidates) {
            val score = candidate.score
            if (score >= max) {
                max = score
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    data class OGTag(val tpe:String, val attribute:String, val name:String, val value:String)

    val ogTags = arrayListOf(
            OGTag("facebook","property","og:image","content"),
            OGTag("facebook","rel","image_src","href"),
            OGTag("twitter","name","twitter:image","value"),
            OGTag("twitter","name","twitter:image","content")
    )

    data class OGImage(val url:String, val tpe:String, var score:Int)
    val largebig = Pattern.compile("(large|big)")

    private fun openGraphResolver(document: Document):String {
        val meta = document.getElementsByTag("meta")
        val links = document.getElementsByTag("link")
        var topImage = ""
        try {
            links?.forEach { meta?.add(it) }
            val ogImages = ArrayList<OGImage>()
            meta?.forEach {
                for (ogTag in ogTags) {
                    val attr = it.attr(ogTag.attribute)
                    val value = it.attr(ogTag.value)
                    if (attr != "" && value != "" && attr == ogTag.name) {
                        val ogImage = OGImage(value!!, ogTag.tpe, 0)
                        ogImages.add(ogImage)
                    }
                }
            }

            if (ogImages.size() == 1) {
                topImage = ogImages.get(0).url
            } else {
                for (ogImage in ogImages) {
                    if (largebig.matcher(ogImage.url).find()) {
                        ogImage.score++
                    }
                    if (ogImage.tpe == "twitter") {
                        ogImage.score++
                    }
                }
                topImage = findBestImageFromScore(ogImages).url
            }

            if (topImage != "" && !topImage.startsWith("http")) {
                topImage = "http://" + topImage
            }
        } finally {
            return topImage
        }
    }

    private fun findBestImageFromScore(ogImages: ArrayList<OGImage>):OGImage {
        var max = 0
        var bestOGIMage = OGImage("","",-1)
        for (ogImage in ogImages) {
            val score = ogImage.score
            if (score >= max) {
                max = score
                bestOGIMage = ogImage
            }
        }
        return bestOGIMage
    }
}
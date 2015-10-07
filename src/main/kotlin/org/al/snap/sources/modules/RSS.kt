package org.al.snap.sources.modules

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.al.snap.sources.camel.Camel
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import java.net.URL
import java.util.*

/**
 * Created by skywalker on 9/22/15.
 */
public class RSS:Module() {
    override fun process(source: Source) {
        handleUrlsEndpoint(source)
    }

    private fun handleUrlsEndpoint(source:Source) {
        val urls = source.params.get("urls")?:""
        if (urls != "") {
            val items = urls.split(",")
            items.forEach { url ->
                info("Handling rss ${url}")
                val timeout = time2ms(source.params.getOrDefault("timeout", "5s"))
                val feed = createFeed(url, timeout.toInt())
                send(feed)
            }
        }
    }

    private fun createFeed(feedUri:String, timeout:Int = 3000): SyndFeed {
        try {
            val url = URL(feedUri)
            val connection = url.openConnection()
            connection.setConnectTimeout(timeout)
            connection.setReadTimeout(10 * timeout)
            val input = SyndFeedInput()
            var feed: SyndFeed? = null
            XmlReader(connection).use {
                feed = input.build(it)
            }
            return feed ?: SyndFeedImpl()
        } catch(e:Exception) {
            warn(e.getMessage()?:"Error in $feedUri")
            return SyndFeedImpl()
        }
    }

    private fun send(feed: SyndFeed, topic:String = "") {
        for (feedEntry in feed.getEntries()) {
            if (feedEntry?.getLink() != null && feedEntry?.getLink() != "") {
                val webEntity = WebEntity()
                webEntity.url = feedEntry?.getLink() ?: ""
                webEntity.finalUrl = feedEntry?.getLink() ?: ""
                webEntity.title = feedEntry?.getTitle() ?: ""
                webEntity.author = feedEntry?.getAuthor() ?: ""
                webEntity.publishedDate = feedEntry?.getPublishedDate() ?: Date()
                webEntity.topic = topic
                webEntity.timestamp = System.currentTimeMillis()
                webEntity.module = "rss"

                info("Send ${webEntity.title} for module ${webEntity.module}}")

                Camel.send(slowEndpoint, webEntity)
            }
        }
    }
}
package org.al.snap.sources.models

import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import org.al.snap.sources.tools.Utils
import org.al.snap.sources.tools.take
import org.jsoup.nodes.Document
import java.io.Serializable
import java.util.*

/**
 * Created by skywalker on 9/17/15.
 */

class WebEntity: Serializable, Utils {
    var valid = true
    var uid = ""
    var url = ""
        set(value) {
            $url = value
            if (uid == "") uid = UID(value)
        }
    var finalUrl = ""
    var domain = ""
    var document: Document? = null
    var html = ""
    var title = ""
        set(value) {
            $title = value
            if (uid == "") uid = UID(value)
        }
    var keywords = ""
    var description = ""
    var content = ""
        set(value) {
            $content = value
            if (uid == "") uid = UID()
        }
    var image = ""
    var author = ""
    var publishedDate = Date()
    var timestamp = Date().getTime()
    var topic = ""
    var error = ""
    var date = Date()
    var exists = false
    var info = ""
    val reserved = ArrayList<String>()

    val links = HashSet<String>()
    var levels = -1
    var numbers = -1
    var external = false
    var timeout = 3000L
    var module = "default"
    var language = ""
    var translation = ""

    override fun toString() = "${url}:${title}"

    fun importJSONString(string:String) {
        val json = JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(string) as JSONObject
        this.url = json.take("url", "")
        this.finalUrl = json.take("final-url", "")
        this.uid = json.take("uid", if (this.finalUrl != "") UID(this.finalUrl) else if (this.url != "") UID(this.url) else UID())

        this.domain = json.take("domain", "")
        this.title = json.take("title", "")
        this.description = json.take("description", "")
        this.keywords = json.take("keywords", "")
        this.content = json.take("content", "")
        this.image = json.take("image", "")
        this.author = json.take("author", "")
        this.publishedDate = json.take("publish-date", Date())
        this.topic = json.take("topic", "")
        this.timestamp = json.take("timestamp", Date().getTime())
        this.module = json.take("module", "")
        this.language = json.take("language", "")
        this.translation = json.take("translation", "")
    }

    fun toJSONString():String {
        val json = JSONObject()
        json.put("url", url)
        json.put("final-url", finalUrl)
        json.put("uid", uid)
        json.put("domain", domain)
        json.put("title", title)
        json.put("description", description)
        json.put("keywords", keywords)
        json.put("content", content)
        json.put("image", image)
        json.put("author", author)
        json.put("publish-date", publishedDate.getTime())
        json.put("topic", topic)
        json.put("timestamp", timestamp)
        json.put("module", module)

        return json.toJSONString()
    }
}
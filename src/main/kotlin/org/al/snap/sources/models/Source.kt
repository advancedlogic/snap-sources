package org.al.snap.sources.models

import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import net.minidev.json.parser.ParseException
import org.al.snap.sources.tools.Globals
import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import org.al.snap.sources.tools.take
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import java.io.Serializable
import java.util.*

/**
 * Created by skywalker on 9/17/15.
 */

public class Source(val uri:String): Serializable, Log, Utils {
    var sid = ""
    var module = ""
    var author = "snap"
    var name = "default"
    var description = ""
    var thumb = ""
    var creationDate = Date()
    var cron = "0 0 * * * ?" //Default is EVERY HOUR
    var status = Globals.SOURCE_READY
    var info = ""
    var params = HashMap<String,String>()
    var format = ""

    var endpoint:String = ""
        set(value) {
            $endpoint = value
            if (value != "") {
                val items = value.split("?")
                if (items.size() > 0) {
                    val left = items[0]
                    if (items.size() > 1) {
                        var right = items[1]
                        val tokens = left.split(":")
                        if (tokens.size() > 0) {
                            this.module = tokens[0]
                            if (tokens.size() > 1) {
                                this.author = tokens[1]
                                if (tokens.size() > 2) {
                                    this.name = tokens[2]
                                }
                            }
                        }
                        if (right != "") {
                            val params = right.split("&")
                            params.forEach {
                                val pair = it.split("=")
                                if (pair.size() == 2) {
                                    this.params.put(pair[0], pair[1])
                                } else {
                                    warn("Error for pair $it")
                                }
                            }
                        }
                    }
                }
            }
        }

    init {
        try {
            val json = JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(uri) as JSONObject
            this.sid = json.take("sid", "")
            this.module = json.take("module", "")
            this.author = json.take("author", "default")
            this.name = json.take("name", UID())
            this.description = json.take("description", "")
            this.thumb = json.take("thumb", "")
            this.creationDate = Date(json.take("timestamp", Date().getTime()))
            this.cron = json.take("cron", this.cron)

            val params = json.take("params", JSONObject())
            for (key in params.keySet()) {
                this.params.put(key, params.getAsString(key))
            }
            this.status = json.take("status", "")
            this.format = "json"
        } catch(e:Exception) {
            this.endpoint = uri
            this.format = "uri"
        }

    }

    override fun toString():String = if (this.format == "json")  toJSON().toJSONString() else toEndpoint()

    public fun toEndpoint():String {
        var sparams = ""
        for ((k,v) in params) {
            sparams += "$k=$v&"
        }
        sparams = sparams.trim('&')
        return "$module:$author:$name?$sparams"
    }

    fun toJSON():JSONObject {
        val json = JSONObject()
        json.put("sid", sid)
        json.put("module", module)
        json.put("author", author)
        json.put("name", name)
        json.put("description", description)
        json.put("thumb", thumb)
        json.put("creation-date", creationDate.getTime())
        json.put("cron", cron)
        json.put("status", status)
        if (info != "") json.put("info", info)

        val jsonParams = JSONObject()
        for ((key,value) in params) {
            jsonParams.put(key, value)
        }
        json.put("params", jsonParams)
        return json
    }
}
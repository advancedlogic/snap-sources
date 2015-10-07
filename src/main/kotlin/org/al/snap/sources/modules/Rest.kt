package org.al.snap.sources.modules

import com.mashape.unirest.http.Unirest
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import org.al.snap.sources.camel.Camel
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.take
import java.util.*

/**
 * Created by upsidedowngalaxy on 9/27/15.
 */
public class Rest:Module() {
    val htmlEndpoint = Conf.getOrDefault("modules.crawler.endpoint", "seda:html")

    override fun process(source: Source) {
        val url = source.params.getOrDefault("urls", "")
        if (url != "") {
            val response = Unirest.get(url).asJson()
            val json = JSONParser(JSONParser.MODE_PERMISSIVE).parse(response.body.toString()) as JSONObject
            for (key in json.keySet()) {
                val array = json.get(key) as JSONArray
                val module = key
                for (i in 0..array.size() - 1) {
                    val jsonObject = array.get(i) as JSONObject
                    val url = jsonObject.take("url", "")
                    val title = jsonObject.take("title", "")
                    val description = jsonObject.take("excerpt", "")
                    val content = jsonObject.take("detail", "")
                    val timestamp = jsonObject.take("date", Date().getTime())
                    val image = jsonObject.take("imageUrl", "")
                    //val score = jsonObject.take("relevancy", 0.0)

                    val webEntity = WebEntity()
                    webEntity.timestamp = timestamp
                    webEntity.date = Date(timestamp)
                    webEntity.module = module
                    webEntity.author = "exo"
                    webEntity.url = url

                    info("Send ${webEntity.url} for module ${webEntity.module}}")

                    if (module != "file") {
                        webEntity.finalUrl = url
                        webEntity.title = title
                        webEntity.description = description
                        webEntity.content = content
                        webEntity.image = image

                        Camel.send(htmlEndpoint, webEntity)
                    } else {
                        Camel.send(slowEndpoint, webEntity)
                    }







                }
            }
        }
    }
}
package org.al.snap.sources.camel.beans

import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import org.al.snap.sources.camel.Camel
import org.al.snap.sources.engine.Crawler
import org.al.snap.sources.engine.Dispatcher
import org.al.snap.sources.engine.Filter
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.stores.DupsCache
import org.al.snap.sources.stores.Elastic
import org.al.snap.sources.stores.FileDB
import org.al.snap.sources.stores.MapDB
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Globals
import java.util.*

/**
 * Created by skywalker on 9/17/15.
 */
public class Service:IService {
    val htmlEndpoint = Conf.getOrDefault("modules.crawler.endpoint", "seda:html")

    fun heartbeat():String {
        return Date().toString()
    }

    fun crawl(url:String) {
        val webEntity = WebEntity()
        webEntity.url = url
        webEntity.module = "link"
        crawl(webEntity)
    }

    fun crawl(webEntity: WebEntity) {
        val url = webEntity.url
        val uid = UID(url)
        val content = webEntity.content
        val start = System.currentTimeMillis()
        info("Crawling ${if (url != "") url else content}")

        if (url == "" && content == "") {
            warn("Empty URL from module ${webEntity.module}")
            return
        }


        Filter.process(webEntity)

        if (webEntity.valid) {
            if (DupsCache.isDuplicate(uid)) {
                warn("[CACHE] url ${url} already processed")
                return
            } else if (Elastic.check("url", url)) {
                warn("[ES] url ${url} already processed")
                return
            }

            Crawler.process(webEntity)
            if ( webEntity.url != "" && (webEntity.content == "" && webEntity.title == "")) {
                warn("$url is empty")
                return
            }


            val stop = System.currentTimeMillis()
            info("Extracted page ${webEntity.title} in ${stop - start}ms")
            webEntity.document = null

            Camel.send(htmlEndpoint, webEntity.toJSONString())
        } else {
            warn("$url not valid")
        }
    }

    override fun create(string: String): JSONObject {
        val source = Source(string)
        if (source.sid != "") {
            return update(source.sid, string);
        } else {
            source.sid = UID("${source.module}:${source.author}:${source.name}")
            if (!FileDB.check("snap-sources", source.sid)) {
                FileDB.store("snap-sources", source.sid, source.toJSON().toString())
            } else {
                source.status = Globals.SOURCE_ERROR
                source.info = "Cannot create source ${source.module}:${source.author}:${source.name}. Already exists. Use update instead."
            }
            return source.toJSON()
        }
    }

    override fun createAndStart(string:String): JSONObject {
        val source = Source(string)
        source.sid = UID()
        FileDB.store("snap-sources", source.sid, source.toJSON().toString())
        return start(source.sid)
    }

    override fun read(sid: String): JSONObject {
        val source = Source(FileDB.load("snap-sources", sid))
        return source.toJSON()
    }

    override fun readAll(): JSONArray {
        val sids = FileDB.loadAllKeys("snap-sources")
        val output = JSONArray()
        sids.forEach { sid -> output.add(read(sid)) }
        return output
    }

    override fun update(sid: String, json: String): JSONObject {
        val source = Source(json)
        source.sid = sid
        FileDB.store("snap-sources", sid, source.toJSON().toString())
        return source.toJSON()
    }

    override fun delete(sid: String): Boolean {
        try {
            FileDB.delete("snap-sources", sid)
            return true
        } catch(e:Exception) {
            warn(e.getMessage()?:"I had some problem deleting $sid")
            return false
        }
    }

    override fun start(sid: String): JSONObject {
        return Dispatcher.startSource(sid)!!.toJSON()
    }

    override fun stop(sid: String): JSONObject {
        return Dispatcher.deleteSource(sid).toJSON()
    }

    override fun suspend(sid: String): JSONObject {
        return Dispatcher.pauseSource(sid).toJSON()
    }

    override fun resume(sid:String):JSONObject {
        return Dispatcher.resumeSource(sid).toJSON()
    }

    override fun disable(sid: String): JSONObject {
        return Dispatcher.deleteSource(sid).toJSON()
    }

    override fun status(sid: String): JSONObject {
        throw UnsupportedOperationException()
    }
}
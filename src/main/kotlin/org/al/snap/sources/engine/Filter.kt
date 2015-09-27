package org.al.snap.sources.engine

import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import java.util.*

/**
 * Created by skywalker on 9/18/15.
 */
public object Filter:Log, Utils {
    val extensions = HashSet<String>()

    fun init() {
        val extensionsBlacklistPath = Conf.getOrDefault("crawler.extensions-blacklist", "conf/extensions-blacklist.txt")
        val file = readFile(extensionsBlacklistPath)
        file.split("\n".toRegex()).toTypedArray().forEach {
            extensions.add(it.trim())
        }
    }

    fun process(entity: WebEntity) {
        if (entity.valid) {
            val url = if (entity.finalUrl == "") entity.url else entity.finalUrl
            if (url != "") {
                val dotIdx = url.lastIndexOf(".")
                if (dotIdx < 0) return
                val extension = url.substring(dotIdx + 1).trim()
                val len = extension.length()
                if (extension in extensions) {
                    entity.valid = false
                } else {
                    if (len >= 5) {
                        for (l in 3..5) {
                            val tmpExtension = extension.substring(0,l)
                            if (tmpExtension in extensions) {
                                entity.valid = false
                            }
                        }
                    }
                }
            }
        }
    }
}
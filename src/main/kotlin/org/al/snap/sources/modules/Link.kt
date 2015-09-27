package org.al.snap.sources.modules

import org.al.snap.sources.camel.Camel
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.take

/**
 * Created by skywalker on 9/22/15.
 */
public class Link:Module() {
    override fun process(source: Source) {
        val url = source.params.getOrDefault("url", "")
        if (url != "") {
            val webEntity = WebEntity()
            webEntity.url = url
            webEntity.timestamp = System.currentTimeMillis()
            webEntity.levels = source.params.getOrDefault("levels", "0").toInt()
            webEntity.numbers = source.params.getOrDefault("numbers", "0").toInt()
            webEntity.module = source.module
            webEntity.timeout = time2ms(source.params.getOrDefault("timeout", "5s"))

            info("Send ${webEntity.url} for module ${webEntity.module}}")


            Camel.send(endpoint, webEntity)
        }
    }
}
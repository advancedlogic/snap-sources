package org.al.snap.sources.modules

import org.al.snap.sources.camel.Camel
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity

/**
 * Created by skywalker on 9/22/15.
 */
public class Domain:Module() {
    override fun process(source: Source) {
        send(source)
    }



    private fun send(source:Source) {
        val urls = source.params.getOrDefault("urls", "")
        if (urls != "") {
            val items = urls.split(",")
            items.forEach { url ->
                val levels = source.params.getOrDefault("levels", "1").toInt()
                val numbers = source.params.getOrDefault("numbers", "-1").toInt()
                val external = source.params.getOrDefault("external", "false").toBoolean()
                val timeout = time2ms(source.params.getOrDefault("timeout", "5s"))

                val webEntity = WebEntity()
                webEntity.url = url
                webEntity.timeout = timeout
                webEntity.levels = levels
                webEntity.numbers = numbers
                webEntity.external = external
                webEntity.module = "domain"

                info("Send ${url} for module ${webEntity.module}}")
                Camel.send(slowEndpoint, webEntity)
            }
        }
    }
}
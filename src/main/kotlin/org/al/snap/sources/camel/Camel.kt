package org.al.snap.sources.camel

import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Log
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.apache.camel.spring.Main
import org.springframework.context.support.FileSystemXmlApplicationContext

/**
 * Created by skywalker on 9/17/15.
 */

public object Camel:Log {
    val camel = Main()
    val springContext = FileSystemXmlApplicationContext(Conf.getOrDefault("camel", "conf/snap-sources.xml"))
    var context: CamelContext? = null
    var template:ProducerTemplate? = null

    fun stop() {
        if (template != null) template?.stop()
        camel.stop()
    }

    fun init() {
        camel.enableHangupSupport()
        val beansOfType=springContext.getBeansOfType(CamelContext::class.java)
        context = if (beansOfType.isEmpty()) null else beansOfType.values().iterator().next()
        camel.setApplicationContext(springContext)
        template = context?.createProducerTemplate()
    }

    fun start() {
        camel.start()
    }

    fun run() {
        camel.run()
    }

    @Suppress("UNCHECKED_CAST")
    fun instance<T>(id:String):T {
        return springContext.getBean(id) as T
    }

    fun send(endpoint:String, entity:Any) {
        template?.sendBody(endpoint, entity)
    }
}
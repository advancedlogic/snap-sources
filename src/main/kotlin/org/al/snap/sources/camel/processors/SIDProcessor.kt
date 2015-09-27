package org.al.snap.sources.camel.processors

import org.apache.camel.Exchange
import org.apache.camel.Processor

/**
 * Created by skywalker on 9/21/15.
 */
public class SIDProcessor : Processor {
    override fun process(exchange: Exchange?) {
        val sid = exchange?.getIn()?.getHeader("sid") as String
        exchange?.getIn()?.setBody(sid)
    }
}
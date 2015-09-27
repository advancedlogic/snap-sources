package org.al.snap.sources.camel.processors

import org.apache.camel.Exchange
import org.apache.camel.Processor

/**
 * Created by skywalker on 9/22/15.
 */
public class URIProcessor : Processor {
    override fun process(exchange: Exchange?) {
        val sid = exchange?.getIn()?.getHeader("uri") as String
        exchange?.getIn()?.setBody(sid)
    }
}
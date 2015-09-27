package org.al.snap.sources.tools

import org.al.snap.sources.stores.LogCache
import org.slf4j.LoggerFactory

/**
 * Created by skywalker on 9/17/15.
 */

public interface Log {
    fun info(message:String) = LoggerFactory.getLogger(this.javaClass)?.info(if (LoggerFactory.getLogger(this.javaClass).isInfoEnabled()) LogCache.push("info", this.javaClass, message) else message)
    fun debug(message:String) = LoggerFactory.getLogger(this.javaClass)?.debug(if (LoggerFactory.getLogger(this.javaClass).isDebugEnabled()) LogCache.push("debug", this.javaClass, message) else message)
    fun warn(message:String) = LoggerFactory.getLogger(this.javaClass)?.warn(if (LoggerFactory.getLogger(this.javaClass).isWarnEnabled()) LogCache.push("warn", this.javaClass, message) else message)
    fun error(message:String) = LoggerFactory.getLogger(this.javaClass)?.error(if (LoggerFactory.getLogger(this.javaClass).isErrorEnabled()) LogCache.push("error", this.javaClass, message) else message)
    fun trace(message:String) = LoggerFactory.getLogger(this.javaClass)?.trace(if (LoggerFactory.getLogger(this.javaClass).isTraceEnabled()) LogCache.push("trace", this.javaClass, message) else message)
}
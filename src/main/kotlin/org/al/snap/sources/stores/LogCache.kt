package org.al.snap.sources.stores

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet

/**
 * Created by skywalker on 9/17/15.
 */

public object LogCache {
    var maxSize:Int = 100
    val cache = ConcurrentLinkedQueue<String>()

    fun push(level:String, jclass:Class<Any>, message:String):String {
        val size = cache.size()
        if (size >= maxSize) {
            cache.poll()
        }
        cache.add("${level}:${Date()}:${jclass.toString()} - ${message}")
        return message
    }

    fun pop(size:Int = 10): ArrayList<String> {
        val output = ArrayList<String>()
        val startIndex = if (cache.size() >= size) cache.size() - size else 0
        for (i in startIndex..cache.size() - 1) {
            output.add(cache.poll())
        }
        return output
    }

    fun clear():Int {
        val size = cache.size()
        cache.clear()
        return size
    }

    fun size():Int = cache.size()
}
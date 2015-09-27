package org.al.snap.sources.stores

import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Environment
import java.util.concurrent.TimeUnit

/**
 * Created by skywalker on 9/18/15.
 */
public object DupsCache {
    val enabled = Conf.getOrDefault("dups-cache.enabled", true)
    val storeName = Conf.getOrDefault("dups-cache.name", "snap-sources-dups")

    fun isDuplicate(key:String):Boolean {
        if (enabled) {
            val map = Environment.hazelcast.getMap<kotlin.String, kotlin.Boolean>("duplicate-${storeName}")
            if (map.contains(key)) return true
            map.put(key, true, 24, TimeUnit.HOURS)
            return false
        }
        return false
    }

    fun free() {
        val map = Environment.hazelcast.getMap<kotlin.String, kotlin.Boolean>("duplicate-${storeName}")
        map.evictAll()
    }

    fun info():Int {
        val map = Environment.hazelcast.getMap<kotlin.String, kotlin.Boolean>("duplicate-${storeName}")
        return map.size()
    }
}
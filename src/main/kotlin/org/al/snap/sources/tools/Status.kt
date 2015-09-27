package org.al.snap.sources.tools

/**
 * Created by skywalker on 9/17/15.
 */

public object Status {
    fun put(key:String, value:String) {
        val map = Environment.hazelcast.getMap<String,String>("snap-status")
        map.put(key, value)
    }

    fun get(key:String):String {
        val map = Environment.hazelcast.getMap<String,String>("snap-status")
        return map.getOrDefault(key, "")
    }

    fun remove(key:String) {
        val map = Environment.hazelcast.getMap<String,String>("snap-status")
        map.removeAsync(key)
    }
}
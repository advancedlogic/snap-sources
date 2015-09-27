package org.al.snap.sources.tools

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import net.minidev.json.JSONObject
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by skywalker on 9/17/15.
 */

public object Environment {
    val SNAP_ENV = "snap-env"
    var hazelcast: HazelcastInstance by Delegates.notNull()

    fun init() {
        hazelcast = Hazelcast.newHazelcastInstance()
    }

    fun put(value:Any, name:String = "default") {
        val map = hazelcast.getMap<kotlin.String, kotlin.Any>(SNAP_ENV)
        map.put(name, value)
    }

    fun get(name:String = "default"):Any? = hazelcast.getMap<kotlin.String, kotlin.Any>(SNAP_ENV).get(name)

    fun purge() {
        hazelcast.getMap<kotlin.String, kotlin.Any>(SNAP_ENV).clear()
    }

    fun remove(name:String = "default") {
        hazelcast.getMap<kotlin.String, kotlin.Any>(SNAP_ENV).removeAsync(name)
    }

    fun list(): HashMap<String, Any> {
        val outputMap = HashMap<String, Any>()
        val map = hazelcast.getMap<kotlin.String, kotlin.Any>(SNAP_ENV)
        for ((key,value) in map) {
            outputMap.put(key, value)
        }
        return outputMap
    }

    fun close() {
        hazelcast.shutdown()
    }

    fun info(): JSONObject {
        val osName = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val osArch = System.getProperty("os.arch")
        val total = Runtime.getRuntime().totalMemory()
        val used = total - Runtime.getRuntime().freeMemory()

        val i = JSONObject()

        i.put("os-name", osName)
        i.put("os-version", osVersion)
        i.put("os-arch", osArch)
        i.put("memory-total", total)
        i.put("memory-used", used)

        return i
    }
}
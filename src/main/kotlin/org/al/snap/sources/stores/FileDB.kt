package org.al.snap.sources.stores

import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

/**
 * Created by upsidedowngalaxy on 9/26/15.
 */
public object FileDB: Log, Utils {
    fun init() {
        FileUtils.forceMkdir(File("sources/"))
    }

    fun check(collection:String, key:String):Boolean {
        return File("sources/$collection/$key.txt").exists()
    }

    fun store(collection:String, key:String, value:String) {
        FileUtils.forceMkdir(File("sources/$collection"))
        File("sources/$collection/$key.txt").writeText(value.toString(), "UTF-8")
    }

    fun delete(collection:String, key:String) {
        File("sources/$collection/$key.txt").delete()
    }

    fun deleteAll<T>(collection:String) {

    }

    fun load(collection:String,key:String):String {
        return File("sources/$collection/$key.txt").readText("UTF-8")
    }

    fun loadAllKeys(collection:String):MutableSet<String> {
        val keys = HashSet<String>()
        File("sources/$collection").list().map { keys.add(it.replace(".txt", "").trim()) }
        return keys
    }

    fun close() {

    }
}
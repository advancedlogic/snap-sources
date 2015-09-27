package org.al.snap.sources.camel.beans

import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import org.al.snap.sources.models.Source
import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import java.util.*

/**
 * Created by skywalker on 9/17/15.
 */

public interface IService: Log, Utils {
    fun create(json:String): JSONObject
    fun createAndStart(string:String): JSONObject
    fun read(sid:String):JSONObject
    fun readAll(): JSONArray
    fun update(sid:String, json:String):JSONObject
    fun delete(sid:String):Boolean

    fun start(sid:String):JSONObject
    fun stop(sid:String):JSONObject
    fun suspend(sid:String):JSONObject
    fun resume(sid:String):JSONObject
    fun disable(sid:String):JSONObject
    fun status(sid:String):JSONObject
}
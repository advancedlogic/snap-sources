package org.al.snap.sources.tools

import net.minidev.json.JSONObject

/**
 * Created by skywalker on 9/17/15.
 */

@Suppress("UNCHECKED_CAST")
fun <T> JSONObject.take(key:String, default:T):T = if (this.containsKey(key)) this.get(key) as T else default

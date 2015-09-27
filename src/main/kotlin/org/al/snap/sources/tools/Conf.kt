package org.al.snap.sources.tools

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import org.slf4j.LoggerFactory

/**
 * Created by skywalker on 9/17/15.
 */
public object Conf:Utils {
    private var configuration: Any? = null

    fun loadConfiguration(path: String) {
        try {
            LoggerFactory.getLogger(this.javaClass)?.info("Load configuration " + path)
            val jsonConfiguration = readFile(path);
            configuration = Configuration.defaultConfiguration().jsonProvider().parse(jsonConfiguration)
        } catch (e: Exception) {
            LoggerFactory.getLogger(this.javaClass)?.error(e.getMessage()!!);
        }
    }

    fun getOrDefault<T>(jsonPath : String, default : T):T {
        var value = default

        try {
            value = JsonPath.read<T>(configuration, "$.${jsonPath}")
        }catch(e:Exception) {
            value = default
        } finally {
            return value
        }
    }
}
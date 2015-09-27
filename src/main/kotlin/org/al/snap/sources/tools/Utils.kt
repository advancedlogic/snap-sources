package org.al.snap.sources.tools

import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*


/**
 * Created by skywalker on 9/17/15.
 */

public interface Utils {

    fun readFile(path:String):String {
        return readFile(path, Charsets.UTF_8)
    }

    fun readFile(path:String, encoding: Charset):String {
        val content = StringBuilder()
        val NL = System.getProperty("line.separator")
        try {
            val fileInputStream = FileInputStream(path)
            val scanner = Scanner(fileInputStream, encoding.name())
            try {
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine() + NL)
                }
            } finally {
                fileInputStream.close()
                scanner.close()
            }
        } catch (e : IOException) {
            error(e.getMessage()!!)
        }
        return content.toString()
    }

    fun cleanDots(string:String):String {
        if (!string.endsWith(".")) return string
        val idx = string.indexOf(".")
        if (idx == (string.length() - 1)) return string.substring(0, string.length() - 1).trim()
        return string
    }

    fun time2ms(time:String):Long {
        if (time.endsWith("mics")) {
            val t = (time.substring(0,time.length() - 4))
            return t.toLong()  / 1000
        }
        if (time.endsWith("ms")) {
            val t = time.substring(0,time.length() - 2)
            return t.toLong()
        }
        if (time.endsWith("s")) {
            val t = time.substring(0,time.length() - 1)
            return t.toLong() * 1000;
        }
        if (time.endsWith("m")) {
            val t = time.substring(0,time.length() - 1)
            return t.toLong() * 60000;
        }
        if (time.endsWith("h")) {
            val t = time.substring(0,time.length() - 1)
            return t.toLong() * 60000 * 60;
        }
        if (time.endsWith("d")) {
            val t = time.substring(0,time.length() - 1)
            return t.toLong() * 60000 * 60 * 24;
        }
        return -1.toLong()
    }

    fun UID():String {
        return UUID.randomUUID().toString()
    }


    fun UID(source:String):String {
        var output = ""
        try {
            val digest = MessageDigest.getInstance("md5")
            digest.update(source.toByteArray(Charsets.UTF_8))
            output = BigInteger(1, digest.digest()!!).toString(16).toLowerCase()
        } finally {
            return output
        }
    }

    @suppress("UNCHECKED_CAST")
    fun sortByValue<T,V>(m: HashMap<T, V>): ArrayList<T> {
        class mapComparator: Comparator<T> {

            override fun compare(o1: T, o2: T): Int {
                val v1 = m.get(o1)
                val v2 = m.get(o2)
                if (v1 == null) {
                    return if (v2 == null) 0 else 1
                } else if (v1 is Comparable<*>) {
                    val tv1 = v1 as Comparable<V>
                    return tv1.compareTo(v2!!)
                } else {
                    return 0
                }
            }
        }

        val keys = ArrayList<T>(m.keySet())
        Collections.sort(keys, mapComparator())
        return keys
    }
}
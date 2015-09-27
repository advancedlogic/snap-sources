package org.al.snap.sources.stores

import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import org.apache.commons.io.FileUtils
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File
import kotlin.properties.Delegates

/**
 * Created by skywalker on 9/17/15.
 */

public object MapDB: Log, Utils {
    var db: DB by Delegates.notNull()

    fun init() {
        FileUtils.forceMkdir(File("sources/"))
        db = DBMaker.fileDB(File("sources/db.dat")).closeOnJvmShutdown().encryptionEnable("snap-sources").makeTxMaker().makeTx()
    }

    fun check<T>(collection:String, key:String):Boolean {
        val map = db.treeMapCreate(collection).makeOrGet<String,T>()
        return map.containsKey(key)
    }

    fun store<T>(collection:String, key:String, value:T) {
        try {
            val map = db.treeMapCreate(collection).makeOrGet<String,T>()
            map.put(key, value)
            db.commit()
        } catch(e:Exception) {
            db.rollback()
        }
    }

    fun delete<T>(collection:String, key:String) {
        try {
            val map = db.treeMapCreate(collection).makeOrGet<String,T>()
            map.remove(key)
            db.commit()
        } catch(e:Exception) {
            db.rollback()
        }
    }

    fun deleteAll<T>(collection:String) {
        try {
            val map = db.treeMapCreate(collection).makeOrGet<String,T>()
            map.clear()
            db.commit()
        } catch(e:Exception) {
            db.rollback()
        }
    }

    fun load<T>(collection:String,key:String):T {
        val map = db.treeMapCreate(collection).makeOrGet<String,T>()
        return map.get(key)!!
    }

    fun loadAllKeys<T>(collection:String):MutableSet<String> {
        val map = db.treeMapCreate(collection).makeOrGet<String,T>()
        return map.keySet()
    }

    fun close() {
        db.close()
    }
}
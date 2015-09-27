package org.al.snap.sources.main

import org.al.snap.sources.camel.Camel
import org.al.snap.sources.engine.Crawler
import org.al.snap.sources.engine.Dispatcher
import org.al.snap.sources.engine.Filter
import org.al.snap.sources.stores.Elastic
import org.al.snap.sources.stores.FileDB
import org.al.snap.sources.stores.MapDB
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Environment
import org.al.snap.sources.tools.Log

/**
 * Created by skywalker on 9/17/15.
 */
public object App {
    fun run() {
        Conf.loadConfiguration("conf/snap-sources.conf")
        Environment.init()
        FileDB.init()
        Elastic.init()
        Filter.init()
        Crawler.init()
        Dispatcher.init()
        Camel.init()

        println(logo)
        Camel.run()
    }

    fun close() {
        Camel.stop()
        Elastic.close()
    }
}

val logo = """

  ///
(o)(o)     ____  _   _    _    ____      ____
(°_°)     / ___|| \ | |  / \  |  _ \    / ___|  ___  _   _ _ __ ___ ___  ___
`(_)      \___ \|  \| | / _ \ | |_) |___\___ \ / _ \| | | | '__/ __/ _ \/ __|
`(_)       ___) | |\  |/ ___ \|  __/_____|__) | (_) | |_| | | | (_|  __/\__ \
“(_)(_)(_)|____/|_| \_/_/   \_\_|       |____/ \___/ \__,_|_|  \___\___||___/

(Social News Analytics Platform - Sources - by UpsidedownGalaxy 2015)

"""

class ShutDownHook(val mainThread:Thread):Runnable, Log {
    override fun run() {
        println("So long and thanks for all the fish!!!")
        App.close()
        mainThread.join()
    }
}

fun main(args:Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread(ShutDownHook(Thread.currentThread())))
    App.run()
}
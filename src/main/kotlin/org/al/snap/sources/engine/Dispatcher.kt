package org.al.snap.sources.engine

import org.al.snap.sources.models.Source
import org.al.snap.sources.modules.Module
import org.al.snap.sources.stores.FileDB
import org.al.snap.sources.stores.MapDB
import org.al.snap.sources.tools.*
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import kotlin.properties.Delegates
import org.quartz.JobBuilder.*
import org.quartz.TriggerBuilder.*
import org.quartz.SimpleScheduleBuilder.*
import java.util.*

/**
 * Created by skywalker on 9/18/15.
 */
public object Dispatcher:Log, Utils {
    val name = Conf.getOrDefault("scheduler.name", "snap-source")
    var scheduler:Scheduler by Delegates.notNull<Scheduler>()
    val modules = HashMap<String, Module>()
    val jobs = HashMap<String, JobDetail>()

    fun init() {
        //Init Modules
        val jsModules = Conf.getOrDefault("modules", LinkedHashMap<String,String>())
        jsModules.keySet().forEach { module ->
            val className = jsModules.get(module)
            val objectInstance = Class.forName(className).newInstance() as Module
            modules.put(module, objectInstance)
        }

        //Init QUARTZ
        val factory = StdSchedulerFactory()
        scheduler = factory.getScheduler()
        scheduler.start()

        resumeAllRunningSources()
    }

     fun startSources() {
        val sids = MapDB.loadAllKeys<String>("snap-sources")
        sids.forEach { sid ->
            sids.forEach { sid ->
                startSource(sid)
            }
        }
    }


    fun startSource(sid:String):Source? {
        val ssource = FileDB.load("snap-sources", sid)
        val source = Source(ssource)
        if (source.status == Globals.SOURCE_READY || source.status == Globals.SOURCE_STOPPED) startSource(source)
        info("Source ${source.name}:${source.author} is in status: ${source.status}")
        return source
    }

    fun startSource(source:Source) {
        val module = source.module
        val clazz = this.modules.get(module)?.javaClass
        val job = newJob(clazz)
                .withIdentity("job-${source.sid}", source.module)
                .build()
        val jdm = job.jobDataMap
        jdm.put("source", source)

        val interval = time2ms(source.params.getOrDefault("pause", "1h"))
        val loop = source.params.getOrDefault("loop", "false").toBoolean()

        var scheduleBuilder:SimpleScheduleBuilder? = null
        if (loop) {
            scheduleBuilder = simpleSchedule().withIntervalInMilliseconds(interval).repeatForever()
        } else {
            scheduleBuilder = simpleSchedule().withIntervalInMilliseconds(interval).withRepeatCount(0)
        }
        val trigger = newTrigger()
                .withIdentity("trigger-${source.sid}", source.module)
                .withSchedule(scheduleBuilder)
                .build()

        jobs.put(source.sid, job)
        scheduler.scheduleJob(job, trigger)
        source.status = Globals.SOURCE_RUNNING
        FileDB.store("snap-sources", source.sid, source.toJSON().toString())
    }

    fun resumeSource(sid:String):Source {
        val ssource = FileDB.load("snap-sources", sid)
        val source = Source(ssource)
        resumeSource(source)
        info("Source ${source.name}:${source.author} is in status: ${source.status}")
        return source
    }

    fun resumeSource(source:Source) {
        val job = jobs.get(source.sid)
        scheduler.resumeJob(job?.key)
        source.status = Globals.SOURCE_RUNNING
        FileDB.store("snap-sources", source.sid, source.toJSON().toString())
    }

    fun pauseSource(sid:String):Source {
        val ssource = FileDB.load("snap-sources", sid)
        val source = Source(ssource)
        pauseSource(source)
        info("Source ${source.name}:${source.author} is in status: ${source.status}")
        return source
    }

    fun pauseSource(source:Source) {
        val job = jobs.get(source.sid)
        scheduler.pauseJob(job?.key)
        source.status = Globals.SOURCE_PAUSED
        FileDB.store("snap-sources", source.sid, source.toJSON().toString())
    }

    fun deleteSource(sid:String):Source {
        val ssource = FileDB.load("snap-sources", sid)
        val source = Source(ssource)
        deleteSource(source)
        info("Source ${source.name}:${source.author} is in status: ${source.status}")
        return source
    }

    fun deleteSource(source:Source) {
        val job = jobs.get(source.sid)
        scheduler.deleteJob(job?.key)
        source.status = Globals.SOURCE_STOPPED
        FileDB.store("snap-sources", source.sid, source.toJSON().toString())
    }

    fun close() {
        scheduler.shutdown()
    }

    fun resumeAllRunningSources() {
        val keys = FileDB.loadAllKeys("snap-sources")
        keys.forEach {
            val source = Source(FileDB.load("snap-sources", it))
            if (source.status == "running") {
                startSource(source)
            }
            if (source.status == "paused") {
                source.status = Globals.SOURCE_STOPPED
                FileDB.store("snap-sources", source.sid, source.toJSON().toString())
            }
        }
    }
}
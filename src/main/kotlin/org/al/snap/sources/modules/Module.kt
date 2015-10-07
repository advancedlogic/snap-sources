package org.al.snap.sources.modules

import org.al.snap.sources.models.Source
import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Globals
import org.al.snap.sources.tools.Log
import org.al.snap.sources.tools.Utils
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * Created by skywalker on 9/22/15.
 */
public abstract class Module: Job, Log, Utils {
    val slowEndpoint = Conf.getOrDefault("modules.endpoint.slow", "seda:slow")
    val fastEndpoint = Conf.getOrDefault("modules.endpoint.fast", "seda:fast")

    override fun execute(context: JobExecutionContext?) {
        val source = context?.jobDetail?.jobDataMap?.get("source") as Source
        if (source.status == Globals.SOURCE_DISABLED) return
        source.status = Globals.SOURCE_RUNNING

        if (source.status == Globals.SOURCE_RUNNING) {
            process(source)
        }
        if (source.status != Globals.SOURCE_DISABLED) {
            source.status = Globals.SOURCE_SLEEPING
        }
    }

    abstract protected fun process(source:Source)
}
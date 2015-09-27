package org.al.snap.sources.tools

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

/**
 * Created by skywalker on 9/17/15.
 */

public class Highlight: ForegroundCompositeConverterBase<ILoggingEvent>() {

    override fun getForegroundColorCode(event: ILoggingEvent): String {
        val level = event.getLevel()
        when (level.toInt()) {
            Level.ERROR_INT -> return ANSIConstants.BOLD + ANSIConstants.RED_FG // same as default color scheme
            Level.WARN_INT -> return ANSIConstants.RED_FG// same as default color scheme
            Level.INFO_INT -> return ANSIConstants.CYAN_FG // use CYAN instead of BLUE
            else -> return ANSIConstants.DEFAULT_FG
        }
    }
}
/**
 * Created by upsidedowngalaxy on 4/9/15.
 */
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.al.snap.sources.tools.Highlight

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.OFF

conversionRule("highlightex",Highlight)

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{dd MMM yyyy;HH:mm:ss.SSS} [%thread] %highlightex(%-5level) %logger{5} - %highlightex(%msg) %n"
    }
}

appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{dd MMM yyyy;HH:mm:ss.SSS} [%thread] %highlightex(%-5level) %logger{5} - %highlightex(%msg) %n"
    }

    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "log/snap-kernel-%d{yyyy-MM}.log"
    }
}

logger("org.al", INFO)
logger("com.jayway.jsonpath", OFF)
logger("com.syncthemall", OFF)
logger("com.gravity", OFF)
logger("org.apache", OFF)
logger("org.perf4j.TimingLogger", OFF)
logger("org.springframework", OFF)
logger("cc.notsoclever", OFF)

root(INFO, ["ROLLING", "STDOUT"])
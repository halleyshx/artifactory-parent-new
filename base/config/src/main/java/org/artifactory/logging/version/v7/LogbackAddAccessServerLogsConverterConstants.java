package org.artifactory.logging.version.v7;

/**
 * @author Shay Bagants
 */
public abstract class LogbackAddAccessServerLogsConverterConstants {

    private LogbackAddAccessServerLogsConverterConstants() {}

    // Main log appender
    static String JFROG_ACCESS =
            "    <appender name=\"JFROG_ACCESS\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
                    "        <File>${artifactory.home}/access/logs/jfrog_access.log</File>\n" +
                    "        <rollingPolicy class=\"ch.qos.logback.core.rolling.FixedWindowRollingPolicy\">\n" +
                    "            <FileNamePattern>${artifactory.home}/access/logs/jfrog_access.%i.log.zip</FileNamePattern>\n" +
                    "            <MinIndex>1</MinIndex>\n" +
                    "            <MaxIndex>9</MaxIndex>\n" +
                    "        </rollingPolicy>\n" +
                    "\n" +
                    "        <triggeringPolicy class=\"ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy\">\n" +
                    "            <MaxFileSize>25MB</MaxFileSize>\n" +
                    "        </triggeringPolicy>\n" +
                    "        <encoder class=\"ch.qos.logback.core.encoder.LayoutWrappingEncoder\">\n" +
                    "            <layout class=\"org.artifactory.logging.layout.BackTracePatternLayout\">\n" +
                    "                <pattern>%date [%thread] [%-5p] \\(%-20c{3}:%L\\) - %m%n</pattern>\n" +
                    "            </layout>\n" +
                    "        </encoder>\n" +
                    "    </appender>";

    // Audit log appender
    static String JFROG_ACCESS_AUDIT =
            "    <appender name=\"JFROG_ACCESS_AUDIT\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
                    "        <File>${artifactory.home}/access/logs/audit.log</File>\n" +
                    "        <encoder class=\"ch.qos.logback.core.encoder.LayoutWrappingEncoder\">\n" +
                    "            <layout class=\"org.artifactory.logging.layout.BackTracePatternLayout\">\n" +
                    "                <pattern>%date %message%n</pattern>\n" +
                    "            </layout>\n" +
                    "        </encoder>\n" +
                    "        <rollingPolicy class=\"ch.qos.logback.core.rolling.FixedWindowRollingPolicy\">\n" +
                    "            <FileNamePattern>${artifactory.home}/access/logs/audit.%i.log.zip</FileNamePattern>\n" +
                    "            <maxIndex>13</maxIndex>\n" +
                    "        </rollingPolicy>\n" +
                    "        <triggeringPolicy class=\"ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy\">\n" +
                    "            <MaxFileSize>25MB</MaxFileSize>\n" +
                    "        </triggeringPolicy>\n" +
                    "    </appender>";

    // New console appender that includes the '[JFrog-Access]' String in it to distinguished between Artifactory logs
    // and JFrog Access logs inside the catalina.out log file
    static String JFROG_ACCESS_CONSOLE =
            "    <appender name=\"JFROG_ACCESS_CONSOLE\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
                    "        <encoder class=\"ch.qos.logback.core.encoder.LayoutWrappingEncoder\">\n" +
                    "            <layout class=\"org.artifactory.logging.layout.BackTracePatternLayout\">\n" +
                    "                <pattern>%date ${artifactory.contextId}[%thread] [JFrog-Access] [%-5p] \\(%-20c{3}:%L\\) - %m%n</pattern>\n" +
                    "            </layout>\n" +
                    "        </encoder>\n" +
                    "    </appender>";


    static String JFROG_ACCESS_LOGGER = "\n    <logger name=\"com.jfrog.access\" additivity=\"false\">\n" +
            "        <level value=\"info\"/>\n" +
            "        <appender-ref ref=\"JFROG_ACCESS\"/>\n" +
            "        <appender-ref ref=\"JFROG_ACCESS_CONSOLE\"/>\n" +
            "    </logger>";

    static String JFROG_ACCESS_TOKEN_AUDITOR_LOGGER =
            "    <logger name=\"com.jfrog.access.server.audit.TokenAuditor\" additivity=\"false\">\n" +
                    "        <level value=\"info\"/>\n" +
                    "        <appender-ref ref=\"JFROG_ACCESS_AUDIT\"/>\n" +
                    "        <appender-ref ref=\"JFROG_ACCESS_CONSOLE\"/>\n" +
                    "    </logger>";
}

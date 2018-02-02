/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.version.v5;

import org.apache.commons.lang.StringUtils;
import org.artifactory.util.StringInputStream;
import org.artifactory.util.XmlUtils;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Adds the audit log appender and logger to logback.xml
 *
 * @author Dan Feldman
 */
public class LogbackAddAuditLogConverter implements XmlConverter {
    // private static final Logger log = LoggerFactory.getLogger(LogbackAddAuditLogConverter.class);
    //TODO [by dan]: Conversion disabled for 4.9.0 until we sort out the audit log's format. [RTFACT-9016]
    private static final Logger log =  NOPLogger.NOP_LOGGER;

    @Override
    public void convert(Document doc) {
        log.info("Starting logback conversion --> Adding audit log.");

        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        List<Element> appenders = root.getChildren("appender", ns);
        for (Element element : appenders) {
            if (element.getAttributeValue("name", ns).equals("AUDIT")) {
                log.info("Audit log appender already exists in logback.xml, skipping conversion");
                return;
            }
        }

        try {
            //Add as last appender
            root.addContent(root.indexOf(appenders.get(appenders.size() - 1)) + 1, getAppenderElement());
        } catch (IOException | JDOMException e) {
            String err = "Error adding the Audit Log's appender to logback.xml:";
            log.error(err + e.getMessage());
            log.debug(err, e);
        }

        Element trafficLogger = null;
        List<Element> loggers = root.getChildren("logger", ns);
        for (Element logger : loggers) {
            if (StringUtils.equals(logger.getAttributeValue("name", ns), "org.artifactory.security.log.AuditLogger")) {
                log.info("Audit log logger config already exists in logback.xml, skipping conversion");
                return;
            } else if (StringUtils.equals(logger.getAttributeValue("name", ns), "org.artifactory.traffic.TrafficLogger")) {
                trafficLogger = logger;
            }
        }
        //Try adding the audit logger after the traffic logger, if something messed up just add it as the last one.
        int indexToAdd = trafficLogger != null ? root.indexOf(trafficLogger)
                : root.indexOf(loggers.get(loggers.size() - 1)) + 1;
        try {
            root.addContent(indexToAdd, getLoggerElement());
            root.addContent(++indexToAdd, new Text("\n    "));
        } catch (IOException | JDOMException e) {
            String err = "Error adding the Audit Log's logger to logback.xml:";
            log.error(err + e.getMessage());
            log.debug(err, e);
        }
    }

    private Element getAppenderElement() throws IOException, JDOMException {
        String appender = "    <appender name=\"AUDIT\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
                "        <File>${artifactory.home}/logs/audit.log</File>\n" +
                "        <encoder>\n" +
                "            <pattern>%date %message%n</pattern>\n" +
                "        </encoder>\n" +
                "        <rollingPolicy class=\"ch.qos.logback.core.rolling.FixedWindowRollingPolicy\">\n" +
                "            <FileNamePattern>${artifactory.home}/logs/audit.%i.log.zip</FileNamePattern>\n" +
                "            <maxIndex>13</maxIndex>\n" +
                "        </rollingPolicy>\n" +
                "        <triggeringPolicy class=\"ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy\">\n" +
                "            <MaxFileSize>25MB</MaxFileSize>\n" +
                "        </triggeringPolicy>\n" +
                "    </appender>";
        return getElement(appender);
    }

    private Element getLoggerElement() throws IOException, JDOMException {
        String logger = "<logger name=\"org.artifactory.security.log.AuditLogger\" additivity=\"false\">\n" +
                "        <level value=\"info\"/>\n" +
                "        <appender-ref ref=\"AUDIT\"/>\n" +
                "    </logger>";
        return getElement(logger);
    }

    private Element getElement(String input) throws IOException, JDOMException {
        SAXBuilder builder = XmlUtils.createSaxBuilder();
        try (InputStream stream = new StringInputStream(input)) {
            Document doc = builder.build(stream);
            Element root = doc.getRootElement();
            return root.detach();
        }
    }
}
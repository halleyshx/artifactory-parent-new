/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.version.v6;

import org.artifactory.convert.XmlConverterTest;

public class LogbackAddAuditLogConverterTest extends XmlConverterTest {

    //TODO [by dan]: Conversion disabled for 4.9.0 until we sort out the audit log's format. [RTFACT-9016]
/*    @Test
    public void elementsMissing() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v5/logback.xml", new LogbackAddAuditLogConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        int loggerCount = 0;
        for (Element logger : root.getChildren("logger", ns)) {
            if (StringUtils.equals(logger.getAttributeValue("name", ns), "org.artifactory.security.log.AuditLogger")) {
                loggerCount++;
                assertEquals(logger.getAttributeValue("additivity", ns), "false");
                assertEquals(logger.getChild("level", ns).getAttributeValue("value", ns), "info");
                assertEquals(logger.getChild("appender-ref", ns).getAttributeValue("ref", ns), "AUDIT");
            }
        }
        assertEquals(loggerCount, 1);

        int appenderCount = 0;
        for (Element appender : root.getChildren("appender", ns)) {
            if (StringUtils.equals(appender.getAttributeValue("name", ns), "AUDIT")) {
                appenderCount++;
                assertEquals(appender.getAttributeValue("class", ns), "ch.qos.logback.core.rolling.RollingFileAppender");
                assertEquals(appender.getChild("File", ns).getText(), "${artifactory.home}/logs/audit.log");
                assertEquals(appender.getChild("encoder", ns).getChild("pattern", ns).getText(), "%date %message%n");
                assertEquals(appender.getChild("rollingPolicy", ns).getAttributeValue("class", ns), "ch.qos.logback.core.rolling.FixedWindowRollingPolicy");
                assertEquals(appender.getChild("rollingPolicy", ns).getChild("FileNamePattern", ns).getText(), "${artifactory.home}/logs/audit.%i.log.zip");
                assertEquals(appender.getChild("rollingPolicy", ns).getChild("maxIndex", ns).getText(), "13");
                assertEquals(appender.getChild("triggeringPolicy", ns).getAttributeValue("class", ns), "ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy");
            }
        }
        assertEquals(appenderCount, 1);

    }

    @Test
    public void elementsExist() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v6/logback.xml", new LogbackAddAuditLogConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        int loggerCount = 0;
        for (Element logger : root.getChildren("logger", ns)) {
            if (StringUtils.equals(logger.getAttributeValue("name", ns), "org.artifactory.security.log.SecurityAuditLogger")) {
                loggerCount++;
            }
        }
        assertEquals(loggerCount, 1);

        int appenderCount = 0;
        for (Element appender : root.getChildren("appender", ns)) {
            if (StringUtils.equals(appender.getAttributeValue("name", ns), "AUDIT")) {
                appenderCount++;
            }
        }
        assertEquals(appenderCount, 1);
    }*/
}
/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.version.v6;

import org.apache.commons.lang.StringUtils;
import org.artifactory.convert.XmlConverterTest;
import org.artifactory.logging.version.v5.LogbackRemoveSupportLogConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class LogbackRemoveSupportLogConverterTest extends XmlConverterTest {

    @Test
    public void supportLogRemoved() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v5/logback.xml", new LogbackRemoveSupportLogConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        int loggerRefCount = 0;
        List<Element> loggers = root.getChildren("logger", ns);
        for (Element logger : loggers) {
            if ("org.artifactory.support".equals(logger.getAttributeValue("name", ns))
                    && logger.getChild("appender-ref", ns) != null) {
                loggerRefCount++;
            }
        }
        assertEquals(loggerRefCount, 0);

        int appenderCount = 0;
        List<Element> appenders = root.getChildren("appender", ns);
        for (Element appender : appenders) {
            if (StringUtils.equals(appender.getAttributeValue("name", ns), "SUPPORT")) {
                appenderCount++;
            }
        }
        assertEquals(appenderCount, 0);
    }
}
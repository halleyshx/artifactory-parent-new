/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.version.v5;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes the support log appender, and the related logger's ref to make the support log output into the
 * standard appender.
 *
 * @author Dan Feldman
 */
public class LogbackRemoveSupportLogConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(LogbackRemoveSupportLogConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting logback conversion --> removing Support log appender.");
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        root.getChildren("appender", ns).stream()
                .filter(appender -> "SUPPORT".equals(appender.getAttributeValue("name", ns)))
                .findFirst()
                .ifPresent(root::removeContent);

        root.getChildren("logger", ns).stream()
                .filter(logger -> "org.artifactory.support".equals(logger.getAttributeValue("name", ns)))
                .findFirst()
                .ifPresent(logger -> logger.removeChild("appender-ref", ns));

        log.info("Remove support appender logback conversion completed.");
    }
}
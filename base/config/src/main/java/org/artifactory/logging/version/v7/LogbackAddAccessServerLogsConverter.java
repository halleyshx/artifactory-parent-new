package org.artifactory.logging.version.v7;

import com.google.common.collect.ImmutableMap;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.artifactory.logging.converter.LogbackConverterUtils.addAppender;
import static org.artifactory.logging.converter.LogbackConverterUtils.addLogger;

/**
 * Add the Access Server main appender, and Audit log appender + two loggers
 *
 * @author Shay Bagants
 */
public class LogbackAddAccessServerLogsConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(LogbackAddAccessServerLogsConverter.class);

    private Map<String, String> appendersToAdd = ImmutableMap.of(
            "JFROG_ACCESS_CONSOLE", LogbackAddAccessServerLogsConverterConstants.JFROG_ACCESS_CONSOLE,
            "JFROG_ACCESS", LogbackAddAccessServerLogsConverterConstants.JFROG_ACCESS,
            "JFROG_ACCESS_AUDIT", LogbackAddAccessServerLogsConverterConstants.JFROG_ACCESS_AUDIT);

    private Map<String, String> loggersToAdd = ImmutableMap.of(
            "com.jfrog.access", LogbackAddAccessServerLogsConverterConstants.JFROG_ACCESS_LOGGER,
            "com.jfrog.access.server.audit.TokenAuditor",
            LogbackAddAccessServerLogsConverterConstants.JFROG_ACCESS_TOKEN_AUDITOR_LOGGER);

    @Override
    public void convert(Document doc) {
        log.info("Starting logback conversion --> Adding logs JFrog Access logs.");
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        // Adding 3 new JFrog Access appenders
        appendersToAdd.forEach((appenderName, appenderValue) -> {
            try {
                addAppender(root, ns, appenderName, appenderValue);
            } catch (IOException | JDOMException e) {
                logError(e, appenderName);
            }
        });

        // Adding two new loggers
        loggersToAdd.forEach((loggerName, loggerValue) -> {
            try {
                addLogger(root, ns, loggerName, loggerValue);
            } catch (IOException | JDOMException e) {
                logError(e, loggerName);

            }
        });

        log.info("JFrog Access logs logback conversion completed.");
    }

    private void logError(Exception e, String elementName) {
        String err = "Error adding the '" + elementName + "' element to logback.xml:";
        log.error(err + e.getMessage());
        log.debug(err, e);
    }
}

package org.artifactory.logging.version.v9;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * @author Dan Feldman
 */
public class LogbackAddMigrationLogsConverterTest extends XmlConverterTest {

    private static final String SHA2_LOGGER_NAME = "org.artifactory.storage.jobs.migration.sha256.Sha256MigrationJob";
    private static final String SHA2_APPENDER_NAME = "SHA256_MIGRATION";

    private static final String PATH_CHECKSUM_LOGGER_NAME = "org.artifactory.storage.jobs.migration.pathchecksum.RepoPathChecksumMigrationJob";
    private static final String PATH_CHECKSUM_APPENDER_NAME = "PATH_CHECKSUM_MIGRATION";

    @Test
    public void addAppendersAndLoggers() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v9/before_sha2Migration_logback.xml", new LogbackAddMigrationLogsConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        // Assert new appenders exists
        assertAppenderExists(root, ns, SHA2_APPENDER_NAME);
        assertAppenderExists(root, ns, PATH_CHECKSUM_APPENDER_NAME);

        // Assert new loggers exists
        assertLoggerExists(root, ns, SHA2_LOGGER_NAME);
        assertLoggerExists(root, ns, PATH_CHECKSUM_LOGGER_NAME);
    }

    private void assertLoggerExists(Element root, Namespace ns, String loggerName) {
        assertTrue(root.getChildren("logger", ns).stream()
                .anyMatch(logger -> logger.getAttributeValue("name", ns).equals(loggerName)),
        "Logger '" + loggerName + "' not found after conversion");
    }

    private void assertAppenderExists(Element root, Namespace ns, String appenderName) {
        assertTrue(root.getChildren("appender", ns).stream()
                  .anyMatch(appender -> appender.getAttributeValue("name", ns).equals(appenderName)),
                "Appender '" + appenderName + "' not found after conversion");
    }
}

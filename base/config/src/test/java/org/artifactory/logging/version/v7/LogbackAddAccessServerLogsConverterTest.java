package org.artifactory.logging.version.v7;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Shay Bagants
 */
public class LogbackAddAccessServerLogsConverterTest extends XmlConverterTest {

    @Test
    public void addAppendersAndLoggers() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v7/before_access_logback.xml",
                new LogbackAddAccessServerLogsConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        // Assert new appenders exists
        List<Element> appenders = root.getChildren("appender", ns);
        List<String> newAppenders = appenders.stream()
                .filter(appender -> appender.getAttributeValue("name", ns).equals("JFROG_ACCESS_CONSOLE") ||
                        appender.getAttributeValue("name", ns).equals("JFROG_ACCESS") ||
                        appender.getAttributeValue("name", ns).equals("JFROG_ACCESS_AUDIT"))
                .map(appender -> appender.getAttribute("name", ns).getValue()).collect(Collectors.toList());

        Assert.assertTrue(newAppenders.contains("JFROG_ACCESS"));
        Assert.assertTrue(newAppenders.contains("JFROG_ACCESS_CONSOLE"));
        Assert.assertTrue(newAppenders.contains("JFROG_ACCESS_AUDIT"));

        // Assert new loggers exists
        List<Element> loggers = root.getChildren("logger", ns);
        List<String> newLoggers = loggers.stream()
                .filter(logger -> logger.getAttributeValue("name", ns).equals("com.jfrog.access") ||
                        logger.getAttributeValue("name", ns).equals("com.jfrog.access.server.audit.TokenAuditor"))
                .map(logger -> logger.getAttribute("name", ns).getValue()).collect(Collectors.toList());

        Assert.assertTrue(newLoggers.contains("com.jfrog.access"));
        Assert.assertTrue(newLoggers.contains("com.jfrog.access.server.audit.TokenAuditor"));
    }
}

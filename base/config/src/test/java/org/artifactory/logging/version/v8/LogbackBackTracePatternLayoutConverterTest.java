package org.artifactory.logging.version.v8;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.artifactory.logging.version.v8.LogbackBackTracePatternLayoutConverter.NEW_LAYOUT_CLASS_NAME;
import static org.artifactory.logging.version.v8.LogbackBackTracePatternLayoutConverter.OLD_LAYOUT_CLASS_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author Yinon Avraham.
 */
public class LogbackBackTracePatternLayoutConverterTest extends XmlConverterTest {

    @Test
    public void testConvert() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v8/logback_before.xml",
                new LogbackBackTracePatternLayoutConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Optional<Element> oldClassName = root.getChildren("appender", ns).stream()
                .flatMap(appender -> appender.getChildren("encoder", ns).stream())
                .flatMap(encoder -> encoder.getChildren("layout", ns).stream())
                .filter(layout -> OLD_LAYOUT_CLASS_NAME.equals(layout.getAttributeValue("class", ns)))
                .findFirst();
        assertFalse(oldClassName.isPresent(), "Old class name is still present - was not converted.");

        long count = root.getChildren("appender", ns).stream()
                .flatMap(appender -> appender.getChildren("encoder", ns).stream())
                .flatMap(encoder -> encoder.getChildren("layout", ns).stream())
                .filter(layout -> NEW_LAYOUT_CLASS_NAME.equals(layout.getAttributeValue("class", ns)))
                .count();
        assertEquals(count, 2, "Number of new layout class name is not as expected.");
    }
}
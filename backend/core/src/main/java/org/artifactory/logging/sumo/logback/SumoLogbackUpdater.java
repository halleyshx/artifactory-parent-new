/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.sumo.logback;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.sumologic.SumoLogicConfigDescriptor;
import org.artifactory.logging.sumo.SumoCategory;
import org.artifactory.util.StringInputStream;
import org.artifactory.util.XmlUtils;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Adds Sumo Logic appender to the logback.xml file
 *
 * @author Shay Yaakov
 */
@Component
public class SumoLogbackUpdater {
    private static final Logger log = LoggerFactory.getLogger(SumoLogbackUpdater.class);

    private static final String SUMO_CONSOLE = "SUMO_CONSOLE";
    private static final String SUMO_ACCESS = "SUMO_ACCESS";
    private static final String SUMO_REQUEST = "SUMO_REQUEST";
    private static final String SUMO_TRAFFIC = "SUMO_TRAFFIC";

    private static final Map<String, Supplier<Element>> appenderElementFactories;
    static {
        Map<String, Supplier<Element>> map = Maps.newHashMap();
        map.put(SUMO_CONSOLE, SumoLogbackUpdater::getConsoleAppenderElement);
        map.put(SUMO_ACCESS, SumoLogbackUpdater::getAccessAppenderElement);
        map.put(SUMO_REQUEST, SumoLogbackUpdater::getRequestAppenderElement);
        map.put(SUMO_TRAFFIC, SumoLogbackUpdater::getTrafficAppenderElement);
        appenderElementFactories = Collections.unmodifiableMap(map);
    }

    private static final Map<String, String> loggerByAppender;
    static {
        Map<String, String> map = Maps.newHashMap();
        map.put(SUMO_CONSOLE, "root");
        map.put(SUMO_ACCESS, "org.artifactory.security.AccessLogger");
        map.put(SUMO_REQUEST, "org.artifactory.traffic.RequestLogger");
        map.put(SUMO_TRAFFIC, "org.artifactory.traffic.TrafficLogger");
        loggerByAppender = Collections.unmodifiableMap(map);
    }

    public SumoLogbackUpdater() {}

    public void update(File logbackConfigFile, UpdateData updateData) throws IOException {
        Document doc = XmlUtils.parse(logbackConfigFile);
        update(doc, updateData);
        Format format = Format.getPrettyFormat();
        format.setIndent("    ");
        format.setLineSeparator(LineSeparator.NL);
        backupAndSaveLogback(logbackConfigFile, XmlUtils.outputString(format, doc));
    }

    private static void update(Document doc, UpdateData updateData) {
        Element root = doc.getRootElement();
        try {
            addOrUpdateAppender(root, SUMO_CONSOLE, updateData);
            addOrUpdateAppender(root, SUMO_ACCESS, updateData);
            addOrUpdateAppender(root, SUMO_TRAFFIC, updateData);
            addOrUpdateAppender(root, SUMO_REQUEST, updateData);
        } catch (Exception e) {
            String err = "Error adding Sumo Logic appenders to logback.xml: ";
            log.error(err + e.getMessage());
            log.debug(err, e);
        }
    }

    private static void addOrUpdateAppender(Element root, String appenderName, UpdateData updateData) {
        Namespace ns = root.getNamespace();
        Element appender = findAppender(root, appenderName);
        if (appender == null && updateData.getSumoConfig().isEnabled()) {
            List<Element> appenders = root.getChildren("appender", ns);
            appender = appenderElementFactories.get(appenderName).get();
            //Add to the end
            int lastAppenderIndex = root.indexOf(appenders.get(appenders.size() - 1));
            root.addContent(lastAppenderIndex + 1, appender);
            addAppenderRefToLogger(root, appenderName);
        }
        if (appender != null) {
            updateAppender(root, appender, updateData);
        }
    }

    private static Element findAppender(Element root, String appenderName) {
        Namespace ns = root.getNamespace();
        List<Element> appenders = root.getChildren("appender", ns);
        return appenders.stream()
                .filter(element -> element.getAttributeValue("name", ns).equals(appenderName))
                .findFirst().orElse(null);
    }

    private static void addAppenderRefToLogger(Element root, String appenderName) {
        Namespace ns = root.getNamespace();
        String loggerName = loggerByAppender.get(appenderName);
        if ("root".equals(loggerName)) {
            Element rootElement = root.getChild("root", ns);
            rootElement.addContent(getElement("<appender-ref ref=\"" + appenderName + "\"/>"));
        } else {
            List<Element> loggers = root.getChildren("logger", ns);
            loggers.stream()
                    .filter(logger -> StringUtils.equals(logger.getAttributeValue("name", ns), loggerName))
                    .forEach(logger -> {
                        logger.addContent(getElement("<appender-ref ref=\"" + appenderName + "\"/>"));
                    });
        }
    }

    private static void updateAppender(Element root, Element appender, UpdateData updateData) {
        SumoLogicConfigDescriptor sumoLogicConfig = updateData.getSumoConfig();
        boolean enabled = sumoLogicConfig.isEnabled() && StringUtils.isNotBlank(sumoLogicConfig.getCollectorUrl());
        updateAppenderStringProperty(root, appender, "enabled", () -> String.valueOf(enabled));
        updateAppenderStringProperty(root, appender, "collectorUrl", sumoLogicConfig::getCollectorUrl);
        updateAppenderElementProperty(root, appender, "proxy", () -> proxyToXmlElement(sumoLogicConfig.getProxy()));
        updateAppenderStringProperty(root, appender, "artifactoryHost", updateData::getArtifactoryHost);
        updateAppenderStringProperty(root, appender, "artifactoryNode", updateData::getArtifactoryNode);
    }

    private static void updateAppenderStringProperty(Element root, Element appender, String propName, Supplier<String> valueSupplier) {
        String value = valueSupplier.get();
        Namespace ns = root.getNamespace();
        Element child = appender.getChild(propName, ns);
        if (child == null) {
            if (value == null) {
                return;
            }
            child = getElement("<" + propName + "/>");
            appender.addContent(child);
        } else if (value == null) {
            appender.removeContent(child);
            return;
        }
        child.setText(value);
    }

    private static void updateAppenderElementProperty(Element root, Element appender, String propName, Supplier<Content> valueSupplier) {
        Namespace ns = root.getNamespace();
        Element child = appender.getChild(propName, ns);
        if (child != null) {
            appender.removeContent(child);
        }
        Content value = valueSupplier.get();
        if (value != null) {
            appender.addContent(value);
        }
    }

    private static Element proxyToXmlElement(ProxyDescriptor proxy) {
        if (proxy == null) {
            return null;
        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ProxyDescriptor.class);
            JAXBElement<ProxyDescriptor> jaxbElement = new JAXBElement<>(new QName("temp-ns", "proxy"), ProxyDescriptor.class, proxy);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            marshaller.marshal(jaxbElement, out);
            String xml = out.toString("utf-8");
            //Remove jaxb decorations (namespace, attributes, etc.)
            xml = xml.replaceFirst("<[^/>]*proxy[^>]*>", "<proxy>");
            xml = xml.replaceFirst("</[^>]*proxy[^>]*>", "</proxy>");
            return getElement(xml);
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element getConsoleAppenderElement() {
        return getSumoAppenderTemplate(SUMO_CONSOLE, SumoCategory.CONSOLE,
                "org.artifactory.logging.layout.BackTracePatternLayout",
                "%date [%thread] [%-5level] \\(%-20c{3}:%L\\) %message%n");
    }

    private static Element getAccessAppenderElement() {
        return getSumoAppenderTemplate(SUMO_ACCESS, SumoCategory.ACCESS, null, "%date %message%n");
    }

    private static Element getRequestAppenderElement() {
        return getSumoAppenderTemplate(SUMO_REQUEST, SumoCategory.REQUEST, null, "%message%n");
    }

    private static Element getTrafficAppenderElement() {
        return getSumoAppenderTemplate(SUMO_TRAFFIC, SumoCategory.TRAFFIC, null, "%message%n");
    }

    private static Element getSumoAppenderTemplate(String name, SumoCategory category, String layoutClass, String pattern) {
        String layoutClassAttribute = layoutClass == null ? "" : " class=\"" + layoutClass + "\"";
        String appender =
                "<appender name=\"" + name + "\" class=\"org.artifactory.logging.sumo.SumoAppender\">" +
                "    <layout" + layoutClassAttribute + ">" +
                "        <pattern>" + pattern + "</pattern>" +
                "    </layout>" +
                "    <category>" + category.getName() + "</category>" +
                "</appender>";
        return getElement(appender);
    }

    private static Element getElement(String input) {
        SAXBuilder builder = XmlUtils.createSaxBuilder();
        try (InputStream stream = new StringInputStream(input)) {
            Document doc = builder.build(stream);
            Element root = doc.getRootElement();
            return root.detach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a backup of the existing logback configuration file and proceeds to save the given content
     *
     * @param content The converter logback.xml content
     */
    private static void backupAndSaveLogback(File originalFile, String content) throws IOException {
        File parentDir = originalFile.getParentFile();
        File logbackConfigFile = new File(parentDir, originalFile.getName());
        File originalBackup = new File(parentDir, "logback.original.xml");
        if (originalBackup.exists()) {
            FileUtils.deleteQuietly(originalBackup);
        }
        FileUtils.moveFile(logbackConfigFile, originalBackup);
        FileUtils.writeStringToFile(logbackConfigFile, content, "utf-8");
    }

    public static class UpdateData {
        private final SumoLogicConfigDescriptor sumoConfig;
        private final String artifactoryHost;
        private final String artifactoryNode;

        public UpdateData(SumoLogicConfigDescriptor sumoConfig, String artifactoryHost, String artifactoryNode) {
            this.sumoConfig = Objects.requireNonNull(sumoConfig, "SumoLogic config descriptor is required");
            this.artifactoryHost = Objects.requireNonNull(artifactoryHost, "Artifactory host is required");
            this.artifactoryNode = artifactoryNode;
        }

        public SumoLogicConfigDescriptor getSumoConfig() {
            return sumoConfig;
        }

        public String getArtifactoryHost() {
            return artifactoryHost;
        }

        public String getArtifactoryNode() {
            return artifactoryNode;
        }
    }
}

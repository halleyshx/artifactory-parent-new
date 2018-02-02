package org.artifactory.version.converter.v207;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deleting an old service admin token in order to create a new access admin token (new approach)
 *
 * @author Noam Shemesh
 */
public class RemoveAccessAdminCredentialsConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(RemoveAccessAdminCredentialsConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Removing access admin token from the config file");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        String[] elementPath = new String[] { "security", "accessClientSettings" };
        String missingElement = null;
        Element parentElement = rootElement;
        Element element = null;
        for (String elementName : elementPath) {
            element = parentElement.getChild(elementName, namespace);
            if (element == null) {
                missingElement = elementName;
                break;
            }
            parentElement = element;
        }

        if (element == null) {
            log.info("Element not found: '{}'. Skipping removing of access admin token",
                    missingElement);
            return;
        }

        boolean deleted = element.removeChild("adminToken", namespace);

        log.info("Finish removing access admin token from the config file. deleted: {}", deleted ? "yes" : "no");
    }
}
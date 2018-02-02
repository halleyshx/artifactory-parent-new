package org.artifactory.version.converter.v182;

import org.artifactory.version.converter.XmlConverter;
import org.artifactory.version.converter.v160.AddonsDefaultLayoutConverterHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yinon Avraham
 */
public class ConanDefaultLayoutConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(ConanDefaultLayoutConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting the conan default repository layout conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        log.debug("Adding conan default repository layout");
        Element repoLayoutsElement = rootElement.getChild("repoLayouts", namespace);
        addConanDefaultLayout(repoLayoutsElement, namespace);

        log.info("Conan default repository layout conversion finished successfully");
    }

    private void addConanDefaultLayout(Element repoLayoutsElement, Namespace namespace) {
        repoLayoutsElement.addContent(
                AddonsDefaultLayoutConverterHelper.getRepoLayoutElement(repoLayoutsElement, namespace,
                        "conan-default",
                        "[module]/[baseRev]@[org]/[channel<[^/]+>][remainder<(?:.*)>]",
                        "false", null,
                        ".*",
                        ".*"));
    }
}

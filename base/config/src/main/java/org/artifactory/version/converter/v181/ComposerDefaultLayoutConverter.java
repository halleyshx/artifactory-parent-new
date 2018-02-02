package org.artifactory.version.converter.v181;

import org.artifactory.version.converter.XmlConverter;
import org.artifactory.version.converter.v160.AddonsDefaultLayoutConverterHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shay Bagants
 */
public class ComposerDefaultLayoutConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(ComposerDefaultLayoutConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting the composer addon repository layout conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        log.debug("Adding composer addon default layouts");
        Element repoLayoutsElement = rootElement.getChild("repoLayouts", namespace);
        addComposerDefaultLayout(repoLayoutsElement, namespace);

        log.info("Ending the composer addon repository layout conversion");
    }

    private void addComposerDefaultLayout(Element repoLayoutsElement, Namespace namespace) {
        repoLayoutsElement.addContent(
                AddonsDefaultLayoutConverterHelper.getRepoLayoutElement(repoLayoutsElement, namespace,
                        "composer-default",
                        "[orgPath]/[module]/[module]-[baseRev](-[fileItegRev]).[ext]",
                        "false", null,
                        ".*",
                        ".*"));
    }
}

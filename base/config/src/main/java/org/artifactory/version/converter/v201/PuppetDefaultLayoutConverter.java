package org.artifactory.version.converter.v201;

import org.artifactory.version.converter.XmlConverter;
import org.artifactory.version.converter.v160.AddonsDefaultLayoutConverterHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jainish shah
 */
public class PuppetDefaultLayoutConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(PuppetDefaultLayoutConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting the puppet repository layout conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        log.debug("Adding puppet default layouts");
        Element repoLayoutsElement = rootElement.getChild("repoLayouts", namespace);
        addPuppetDefaultLayout(repoLayoutsElement, namespace);

        log.info("Ending the puppet repository layout conversion");
    }

    private void addPuppetDefaultLayout(Element repoLayoutsElement, Namespace namespace) {
        repoLayoutsElement.addContent(
                AddonsDefaultLayoutConverterHelper.getRepoLayoutElement(repoLayoutsElement, namespace,
                        "puppet-default",
                        "[orgPath]/[module]/[orgPath]-[module]-[baseRev].tar.gz",
                        "false", null,
                        ".*",
                        ".*"));
    }
}

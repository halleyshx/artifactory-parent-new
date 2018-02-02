package org.artifactory.version.converter.v181;

import org.artifactory.version.converter.v160.AddonsLayoutConverterTestBase;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * @author Shay Bagants
 */
@Test
public class ComposerDefaultLayoutConverterTest extends AddonsLayoutConverterTestBase {

    // Convert the previous descriptor and ensure that all addon layouts (including composer) are now exists
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config.1.8.0_no_composer_layout.xml",
                new ComposerDefaultLayoutConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element repoLayoutsElement = rootElement.getChild("repoLayouts", namespace);
        checkForDefaultLayouts(repoLayoutsElement, namespace);
    }

    // Convert an older descriptor (1.7.5) and ensure that all addon layouts (including composer) are now exists as well
    public void convert175Descriptor() throws Exception {
        Document document = convertXml("/config/test/config.1.7.5_docker_force_auth.xml",
                new ComposerDefaultLayoutConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element repoLayoutsElement = rootElement.getChild("repoLayouts", namespace);
        checkForDefaultLayouts(repoLayoutsElement, namespace);
    }

    private void checkForDefaultLayouts(Element repoLayoutsElement, Namespace namespace) {
        List<Element> repoLayoutElements = repoLayoutsElement.getChildren();

        assertNotNull(repoLayoutElements, "Converted configuration should contain default repo layouts.");
        assertFalse(repoLayoutElements.isEmpty(),
                "Converted configuration should contain default repo layouts.");

        checkForDefaultNuGetLayout(repoLayoutElements, namespace);
        checkForDefaultNpmLayout(repoLayoutElements, namespace);
        checkForDefaultBowerLayout(repoLayoutElements, namespace);
        checkForDefaultVcsLayout(repoLayoutElements, namespace);
        checkForDefaultSbtLayout(repoLayoutElements, namespace);
        checkForDefaultSimpleLayoutAfterVer460(repoLayoutElements, namespace);
        checkForDefaultComposerLayout(repoLayoutElements, namespace);
    }
}

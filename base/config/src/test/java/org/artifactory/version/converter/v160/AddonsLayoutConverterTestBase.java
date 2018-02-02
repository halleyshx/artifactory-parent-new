package org.artifactory.version.converter.v160;

import org.apache.commons.lang.StringUtils;
import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Base class for the layout converters tests
 *
 * @author Shay Bagants
 */
public class AddonsLayoutConverterTestBase extends XmlConverterTest {

    public void checkForDefaultNuGetLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "nuget-default",
                "[orgPath]/[module]/[module].[baseRev](-[fileItegRev]).nupkg",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultNpmLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "npm-default",
                "[orgPath]/[module]/[module]-[baseRev](-[fileItegRev]).tgz",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultBowerLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "bower-default",
                "[orgPath]/[module]/[module]-[baseRev](-[fileItegRev]).[ext]",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultVcsLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "vcs-default",
                "[orgPath]/[module]/[refs<tags|branches>]/[baseRev]/[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]",
                "false",
                null,
                ".*",
                "[a-zA-Z0-9]{40}");
    }

    public void checkForDefaultSbtLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "sbt-default",
                "[org]/[module]/(scala_[scalaVersion<.+>])/(sbt_[sbtVersion<.+>])/[baseRev]/[type]s/[module](-[classifier]).[ext]",
                "true",
                "[org]/[module]/(scala_[scalaVersion<.+>])/(sbt_[sbtVersion<.+>])/[baseRev]/[type]s/ivy.xml",
                "\\d{14}",
                "\\d{14}");
    }

    public void checkForDefaultSimpleLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "simple-default",
                "[orgPath]/[module]/[module]-[baseRev](-[fileItegRev]).[ext]",
                "false",
                null,
                ".*",
                ".*");
    }

    // In version 4.6.0 there is a converter to remote the 'fileItegRev'
    public void checkForDefaultSimpleLayoutAfterVer460(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "simple-default",
                "[orgPath]/[module]/[module]-[baseRev].[ext]",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultComposerLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "composer-default",
                "[orgPath]/[module]/[module]-[baseRev](-[fileItegRev]).[ext]",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultPuppetLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "puppet-default",
                "[orgPath]/[module]/[orgPath]-[module]-[baseRev].tar.gz",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkForDefaultConanLayout(List<Element> repoLayoutElements, Namespace namespace) {
        checkLayout(repoLayoutElements, namespace, "conan-default",
                "[module]/[baseRev]@[org]/[channel<[^/]+>][remainder<(?:.*)>]",
                "false",
                null,
                ".*",
                ".*");
    }

    public void checkLayout(List<Element> repoLayoutElements, Namespace namespace, String layoutName,
            String artifactPathPattern, String distinctiveDescriptorPathPattern, String descriptorPathPattern,
            String folderIntegrationRevisionRegExp, String fileIntegrationRevisionRegExp) {

        boolean foundLayout = false;
        for (Element repoLayoutElement : repoLayoutElements) {
            if (layoutName.equals(repoLayoutElement.getChild("name", namespace).getText())) {
                checkLayoutElement(repoLayoutElement, namespace, layoutName, artifactPathPattern,
                        distinctiveDescriptorPathPattern, descriptorPathPattern, folderIntegrationRevisionRegExp,
                        fileIntegrationRevisionRegExp);
                foundLayout = true;
            }
        }
        assertTrue(foundLayout, "Could not find the default layout: " + layoutName);
    }

    private void checkLayoutElement(Element repoLayoutElement, Namespace namespace, String layoutName,
            String artifactPathPattern, String distinctiveDescriptorPathPattern, String descriptorPathPattern,
            String folderIntegrationRevisionRegExp, String fileIntegrationRevisionRegExp) {

        checkLayoutField(repoLayoutElement, namespace, layoutName, "artifactPathPattern", artifactPathPattern,
                "artifact path pattern");

        checkLayoutField(repoLayoutElement, namespace, layoutName, "distinctiveDescriptorPathPattern",
                distinctiveDescriptorPathPattern, "distinctive descriptor path pattern");

        if (StringUtils.isNotBlank(descriptorPathPattern)) {
            checkLayoutField(repoLayoutElement, namespace, layoutName, "descriptorPathPattern", descriptorPathPattern,
                    "descriptor path pattern");
        } else {
            assertNull(repoLayoutElement.getChild("descriptorPathPattern"));
        }

        checkLayoutField(repoLayoutElement, namespace, layoutName, "folderIntegrationRevisionRegExp",
                folderIntegrationRevisionRegExp, "folder integration revision reg exp");

        checkLayoutField(repoLayoutElement, namespace, layoutName, "fileIntegrationRevisionRegExp",
                fileIntegrationRevisionRegExp, "file integration revision reg exp");
    }

    private void checkLayoutField(Element repoLayoutElement, Namespace namespace, String layoutName, String childName,
            String expectedChildValue, String childDisplayName) {
        Element childElement = repoLayoutElement.getChild(childName, namespace);
        assertNotNull(childElement, "Could not find " + childDisplayName + " element in default repo layout: " +
                layoutName);
        assertEquals(childElement.getText(), expectedChildValue, "Unexpected " + childDisplayName +
                " in default repo layout: " + layoutName);
    }
}

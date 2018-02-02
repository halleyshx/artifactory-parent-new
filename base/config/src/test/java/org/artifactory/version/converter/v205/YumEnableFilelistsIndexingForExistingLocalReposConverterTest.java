package org.artifactory.version.converter.v205;

import org.artifactory.convert.XmlConverterTest;
import org.artifactory.version.converter.v205.YumEnableFilelistsIndexingForExistingLocalReposConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;
import org.testng.collections.Lists;


import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dan Feldman
 */
public class YumEnableFilelistsIndexingForExistingLocalReposConverterTest extends XmlConverterTest {

    @Test
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config.2.0.4.enable_calculate_yum_filelists.xml",
                new YumEnableFilelistsIndexingForExistingLocalReposConverter());

        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        //Existing local repositories should contain a new tag enabling auto calculation of filelists.xml to ceep old behavior
        validateNewTagAdded(rootElement, namespace,
                Lists.newArrayList("generic-local", "example-repo-local", "ggggggg", "new-rpm", "rpm-local", "rpm-local-noFileList", "rrrrr"));

    }

    private void validateNewTagAdded(Element rootElement, Namespace namespace, List<String> repos) {
        List<Element> localRepos = rootElement.getChild("localRepositories", namespace).getChildren();

        localRepos.stream()
                .filter(repo -> repos.contains(repo.getChild("key", namespace).getText()))
                .forEach(repo -> {
                    Element isCalculateFilelists = repo.getChild("enableFileListsIndexing", namespace);
                    assertNotNull(isCalculateFilelists);
                    assertTrue(Boolean.parseBoolean(isCalculateFilelists.getText()));
                });
    }



}

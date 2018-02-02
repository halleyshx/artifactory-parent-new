package org.artifactory.version.converter.v205;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Liza Dashevski
 */
public class YumEnableFilelistsIndexingForExistingLocalReposConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(YumEnableFilelistsIndexingForExistingLocalReposConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting The Yum local repositories conversion, to enable existing local repositories to calculate filelists.xml ");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        convertRepos(rootElement.getChild("localRepositories", namespace), namespace);
        log.info("Finish The Yum local repositories conversion, to enable existing local repositories to calculate filelists.xml ");
    }

    private void convertRepos(Element repos, Namespace namespace) {
        if (repos != null && repos.getChildren() != null && !repos.getChildren().isEmpty()) {
            repos.getChildren().forEach(repo -> insertEnableYumFilelistsCalculation(repo, namespace));
        }
    }

    private void insertEnableYumFilelistsCalculation(Element repoElement, Namespace namespace) {
        Element calculateFilelists = new Element("enableFileListsIndexing", namespace)
                .setText(String.valueOf(true));
        int lastLocation = findLocationToInsert(repoElement);
        if (lastLocation < 0) {
            log.error("Failed to find a correct location to insert Yum calculate filelists xml meta data file " +
                            "for repo {}, with enabled",
                    repoElement.getChild("key", namespace).getText());
        } else {
            repoElement.addContent(lastLocation + 1, new Text("\n            "));
            repoElement.addContent(lastLocation + 2, calculateFilelists);
            repoElement.addContent(lastLocation + 3, new Text("\n        "));
        }
    }

    //Has to be inserted as the last element in the RealRepo schema
    private int findLocationToInsert(Element repo) {
        return findLastLocation(repo,
                "key",
                "type",
                "description",
                "notes",
                "includesPattern",
                "excludesPattern",
                "repoLayoutRef",
                "dockerApiVersion",
                "forceNugetAuthentication",
                "blackedOut",
                "handleReleases",
                "handleSnapshots",
                "maxUniqueSnapshots",
                "maxUniqueTags",
                "suppressPomConsistencyChecks",
                "propertySets",
                "archiveBrowsingEnabled",
                "xray",
                "snapshotVersionBehavior",
                "localRepoChecksumPolicyType",
                "calculateYumMetadata",
                "yumRootDepth",
                "yumGroupFileNames",
                "debianTrivialLayout");
    }

    @Override
    public int findLastLocation(Element parent, String... elements) {
        int lastIndex = -1;
        for (String element : elements) {
            Element child = parent.getChild(element, parent.getNamespace());
            if (child != null) {
                int childIndex = parent.indexOf(child);
                lastIndex = childIndex > lastIndex ? childIndex : lastIndex;
            }
        }
        return lastIndex;
    }
}
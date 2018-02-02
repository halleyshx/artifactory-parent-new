/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.version.converter.v175;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shay Bagants
 */
public class DockerForceAuthRemovalConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(DockerForceAuthRemovalConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting docker force authentication removal conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        convertRepos(rootElement.getChild("localRepositories", namespace), namespace);
        convertRepos(rootElement.getChild("remoteRepositories", namespace), namespace);
        convertRepos(rootElement.getChild("virtualRepositories", namespace), namespace);
        convertRepos(rootElement.getChild("distributionRepositories", namespace), namespace);
        log.info("Finished docker force authentication removal conversion");
    }

    private void convertRepos(Element repos, Namespace namespace) {
        if (repos != null && repos.getChildren() != null && !repos.getChildren().isEmpty()) {
            repos.getChildren().forEach(repoElement -> removeForceAuthTag(repoElement, namespace));
        }
    }

    private void removeForceAuthTag(Element repoElement, Namespace namespace) {
        Element forceDockerAuth = repoElement.getChild("forceDockerAuthentication", namespace);
        if (forceDockerAuth != null) {
            String repoKey = repoElement.getChild("key", namespace).getText();
            log.debug("Removing the '{}' tag from '{}'", "forceDockerAuthentication", repoKey);
            repoElement.removeChild("forceDockerAuthentication", namespace);
        }
    }
}

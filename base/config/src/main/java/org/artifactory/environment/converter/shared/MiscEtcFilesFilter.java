package org.artifactory.environment.converter.shared;

import org.artifactory.common.ArtifactoryHome;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Dan Feldman
 * A {@link FileFilter} that accepts files called:
 * artifactory.gpg.public
 * artifactory.gpg.private
 * artifactory.ssh.public
 * artifactory.ssh.private
 * artifactory.config.* (but not config.import)
 */
public class MiscEtcFilesFilter implements FileFilter {

    private static final String CONFIG_XMLS = "artifactory.config.*";

    @Override
    public boolean accept(File pathname) {
        return pathname != null
                && (pathname.getAbsolutePath().contains(ArtifactoryHome.ARTIFACTORY_GPG_PUBLIC_KEY)
                || pathname.getAbsolutePath().contains(ArtifactoryHome.ARTIFACTORY_GPG_PRIVATE_KEY)
                || pathname.getAbsolutePath().contains(ArtifactoryHome.ARTIFACTORY_SSH_PUBLIC_KEY)
                || pathname.getAbsolutePath().contains(ArtifactoryHome.ARTIFACTORY_SSH_PRIVATE_KEY)
                // Don't copy over config.import files, don't want to accidentally overwrite stuff
                || (pathname.getAbsolutePath().contains(CONFIG_XMLS) && !pathname.getAbsolutePath().contains(".import")));
    }
}

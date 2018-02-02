package org.artifactory.environment.converter.shared.version.v1;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Gidi Shabat
 */
public class NoNfsUIConverter extends NoNfsBasicEnvironmentConverter {
    private static final Logger log = LoggerFactory.getLogger(NoNfsUIConverter.class);

    @Override
    protected void doConvert(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            log.info("Starting environment conversion: copy ui dir from cluster home to node home");
            safeMoveDirectory(clusterHomeDir, artifactoryHome.getLogoDir());
            log.info("Finished environment conversion: copy ui dir from cluster home to node home");
        }
    }

    @Override
    protected void doAssertConversionPreconditions(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            File oldDir = getOldHaFile(clusterHomeDir, artifactoryHome.getLogoDir());
            assertFilePermissions(oldDir);
            assertTargetFilePermissions(artifactoryHome.getLogoDir());
        }
    }

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return isUpgradeTo5x(source, target);
    }
}
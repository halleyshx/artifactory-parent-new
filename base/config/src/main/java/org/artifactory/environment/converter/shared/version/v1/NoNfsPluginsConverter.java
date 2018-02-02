package org.artifactory.environment.converter.shared.version.v1;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Gidi Shabat
 */
public class NoNfsPluginsConverter extends NoNfsBasicEnvironmentConverter {
    private static final Logger log = LoggerFactory.getLogger(NoNfsPluginsConverter.class);

    @Override
    protected void doConvert(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            log.info("Starting environment conversion: copy plugins folder from cluster home to node home");
            safeMoveDirectory(clusterHomeDir, artifactoryHome.getPluginsDir());
            log.info("Finished environment conversion: copy plugins folder from cluster home to node home");
        }
    }

    @Override
    protected void doAssertConversionPreconditions(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            File oldHaFile = getOldHaFile(clusterHomeDir, artifactoryHome.getPluginsDir());
            File[] listFiles = oldHaFile.listFiles();
            if (listFiles != null) {
                for (File candidateFile : listFiles) {
                    assertFilePermissions(oldHaFile);
                    assertTargetFilePermissions(
                            new File(artifactoryHome.getPluginsDir(), candidateFile.getName()));
                }
            }
        }
    }

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return isUpgradeTo5x(source, target);
    }
}
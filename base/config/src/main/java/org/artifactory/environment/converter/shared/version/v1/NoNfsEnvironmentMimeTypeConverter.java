package org.artifactory.environment.converter.shared.version.v1;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Gidi Shabat
 */
public class NoNfsEnvironmentMimeTypeConverter extends NoNfsBasicEnvironmentConverter {
    private static final Logger log = LoggerFactory.getLogger(NoNfsEnvironmentMimeTypeConverter.class);

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return isUpgradeTo5x(source, target);
    }

    @Override
    protected void doConvert(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            log.info("Starting environment conversion: copy mimetypes.xml from cluster home to node home");
            safeCopyRelativeFile(clusterHomeDir, artifactoryHome.getMimeTypesFile());
            log.info("Finished environment conversion: copy mimetypes.xml from cluster home to node home");
        }
    }

    @Override
    protected void doAssertConversionPreconditions(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (clusterHomeDir != null) {
            File oldHaFile = getOldHaFile(clusterHomeDir, artifactoryHome.getMimeTypesFile());
            assertFilePermissions(oldHaFile);
            assertTargetFilePermissions(artifactoryHome.getMimeTypesFile());
        }
    }
}
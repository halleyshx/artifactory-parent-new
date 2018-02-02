package org.artifactory.environment.converter.local.version.v1;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converter.ConverterPreconditionException;
import org.artifactory.environment.converter.BasicEnvironmentConverter;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.common.logging.BootstrapLogger;

import java.io.File;

import static org.artifactory.environment.converter.shared.version.v1.NoNfsBasicEnvironmentConverter.*;

/**
 * @author Gidi Shabat
 * @author Dan Feldman
 */
public class NoNfsArtifactorySystemPropertiesConverter implements BasicEnvironmentConverter {

    @Override
    public void convert(ArtifactoryHome artifactoryHome, CompoundVersionDetails source, CompoundVersionDetails target) {
        File clusterHomeDir = resolveClusterHomeDir(artifactoryHome);
        if (clusterHomeDir != null) {
            safeCopyRelativeFile(clusterHomeDir, artifactoryHome.getArtifactorySystemPropertiesFile());
            try {
                artifactoryHome.initArtifactorySystemProperties();
            } catch (Exception e) {
                BootstrapLogger.warn("Failed to re-init artifactory.system.properties after copy from cluster etc folder.");
            }
        }
    }

    @Override
    public void assertConversionPreconditions(ArtifactoryHome home) throws ConverterPreconditionException {
        File clusterHomeDir = resolveClusterHomeDir(home);
        if (clusterHomeDir != null) {
            assertFilePermissions(getOldHaFile(clusterHomeDir, home.getArtifactorySystemPropertiesFile()));
        }
    }

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return isUpgradeTo5x(source, target);
    }
}

package org.artifactory.environment.converter.local;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converter.ArtifactoryConverterAdapter;
import org.artifactory.converter.ConverterPreconditionException;
import org.artifactory.environment.converter.local.version.LocalEnvironmentVersion;
import org.artifactory.version.CompoundVersionDetails;

import javax.annotation.Nullable;

/**
 * @author Gidi Shabat
 */
public class PreInitConverter implements ArtifactoryConverterAdapter {

    private ArtifactoryHome artifactoryHome;


    public PreInitConverter(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
    }

    @Override
    public boolean isInterested(@Nullable CompoundVersionDetails source, CompoundVersionDetails target) {
        return true;
    }

    @Override
    public void revert() {

    }

    @Override
    public void backup() {

    }

    @Override
    public void clean() {

    }

    @Override
    public void assertConversionPrecondition(ArtifactoryHome home, CompoundVersionDetails fromVersion, CompoundVersionDetails toVersion) throws ConverterPreconditionException {
        if (artifactoryHome.getArtifactoryProperties() == null) {
            artifactoryHome.initArtifactorySystemProperties();
        }
        LocalEnvironmentVersion.assertPreConditions(home, fromVersion, toVersion);
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        try {
            // TODO: [by fsi] Hack here to protect conversion of not init homes
            if (artifactoryHome.getArtifactoryProperties() == null) {
                artifactoryHome.initArtifactorySystemProperties();
            }

            LocalEnvironmentVersion.convert(artifactoryHome, source, target);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute local environment conversion: " + e.getMessage(), e);
        }
    }
}
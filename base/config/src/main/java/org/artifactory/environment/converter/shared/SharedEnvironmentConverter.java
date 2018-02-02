package org.artifactory.environment.converter.shared;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converter.ArtifactoryConverterAdapter;
import org.artifactory.environment.converter.shared.version.SharedEnvironmentVersion;
import org.artifactory.version.CompoundVersionDetails;

/**
 * @author Dan Feldman
 */
public class SharedEnvironmentConverter implements ArtifactoryConverterAdapter {

    private ArtifactoryHome artifactoryHome;

    public SharedEnvironmentConverter(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        try {
            SharedEnvironmentVersion.getCurrent().convert(artifactoryHome, source, target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute shared environment conversion: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        return source != null && !source.isCurrent();
    }

    @Override
    public void backup() {

    }

    @Override
    public void clean() {
    }

    @Override
    public void assertConversionPrecondition(ArtifactoryHome home, CompoundVersionDetails fromVersion,
            CompoundVersionDetails toVersion) {
        SharedEnvironmentVersion.getCurrent().assertPreConditions(home, fromVersion, toVersion);
    }

    @Override
    public void revert() {
    }
}
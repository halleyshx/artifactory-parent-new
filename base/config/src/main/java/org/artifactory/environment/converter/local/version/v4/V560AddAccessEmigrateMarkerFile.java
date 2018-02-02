package org.artifactory.environment.converter.local.version.v4;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converter.ConverterPreconditionException;
import org.artifactory.environment.converter.BasicEnvironmentConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;

import java.io.IOException;

/**
 * @author Noam Shemesh
 */
public class V560AddAccessEmigrateMarkerFile implements BasicEnvironmentConverter {
    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return (source.getVersion().before(ArtifactoryVersion.v560m001) &&
                target.getVersion().afterOrEqual(ArtifactoryVersion.v560m001));
    }

    @Override
    public void convert(ArtifactoryHome artifactoryHome, CompoundVersionDetails source, CompoundVersionDetails target) {
        try {
            // write this file on non-ha env, or on HA primary node.
            if (!artifactoryHome.isHaConfigured() ||
                    (artifactoryHome.getHaNodeProperties() != null && artifactoryHome.getHaNodeProperties().isPrimary())) {
                FileUtils.write(artifactoryHome.getAccessEmigrateMarkerFile(), "");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't write access emigrate marker file", e);
        }
    }

    @Override
    public void assertConversionPreconditions(ArtifactoryHome home) throws ConverterPreconditionException {
    }
}

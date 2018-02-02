package org.artifactory.environment.converter.local.version;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.environment.converter.BasicEnvironmentConverter;
import org.artifactory.environment.converter.local.version.v1.NoNfsArtifactorySystemPropertiesConverter;
import org.artifactory.environment.converter.local.version.v1.NoNfsArtifactoryEncryptionKeysConverter;
import org.artifactory.environment.converter.local.version.v1.NoNfsNewDbPropertiesConverter;
import org.artifactory.environment.converter.local.version.v2.NoNfsArtifactoryPropertiesConverter;
import org.artifactory.environment.converter.local.version.v3.V550UpgradePrerequisiteVerifier;
import org.artifactory.environment.converter.local.version.v4.V560AddAccessEmigrateMarkerFile;
import org.artifactory.version.CompoundVersionDetails;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Local environment converters ALWAYS RUN - If you happen to write one make it smart so it knows if it should run!
 *
 * @author Dan Feldman
 */
public enum LocalEnvironmentVersion {

    //v5.0.0
    v1(new NoNfsNewDbPropertiesConverter(),
            new NoNfsArtifactorySystemPropertiesConverter(),
            new NoNfsArtifactoryEncryptionKeysConverter()),
    //v5.4.0
    v2(new NoNfsArtifactoryPropertiesConverter()),
    //v5.5.0
    v3(new V550UpgradePrerequisiteVerifier()),
    //v5.6.0
    v4(new V560AddAccessEmigrateMarkerFile());
    private final BasicEnvironmentConverter[] converters;

    /**
     * @param converters the converters to run in order to bring the environment to the expected state of this
     *                   environment version
     */
    LocalEnvironmentVersion(BasicEnvironmentConverter... converters) {
        this.converters = converters;
    }

    public static void convert(ArtifactoryHome artifactoryHome, CompoundVersionDetails source, CompoundVersionDetails target) {
        for (LocalEnvironmentVersion environmentVersion : values()) {
            for (BasicEnvironmentConverter basicEnvironmentConverter : environmentVersion.converters) {
                if (basicEnvironmentConverter.isInterested(artifactoryHome, source, target)) {
                    basicEnvironmentConverter.convert(artifactoryHome, source, target);
                }
            }
        }
    }

    /**
     * Assert that the all the converters meet their preconditions before starting the conversions
     */
    public static void assertPreConditions(ArtifactoryHome artifactoryHome, CompoundVersionDetails source,
            CompoundVersionDetails target) {
        Arrays.stream(LocalEnvironmentVersion.values())
                .flatMap(version -> Stream.of(version.converters))
                .filter(converter -> converter.isInterested(artifactoryHome, source, target))
                .forEach(converter -> converter.assertConversionPreconditions(artifactoryHome));
    }
}
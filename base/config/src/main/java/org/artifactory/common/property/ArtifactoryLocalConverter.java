package org.artifactory.common.property;

/**
 * Local Converters are smart and know when and if they should run, this is why they do not require version information
 * @author Dan Feldman
 */
public interface ArtifactoryLocalConverter {

    void convert();
}

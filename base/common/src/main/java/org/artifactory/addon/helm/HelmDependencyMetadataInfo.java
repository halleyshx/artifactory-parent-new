package org.artifactory.addon.helm;

import lombok.Data;

/**
 * @author nadavy
 */
@Data
public class HelmDependencyMetadataInfo {

    public String name;
    public String version;
    public String repository;

    HelmDependencyMetadataInfo(String name, String version, String repository) {
        this.name = name;
        this.version = version;
        this.repository = repository;
    }
}

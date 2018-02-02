package org.artifactory.addon.helm;

import lombok.Data;

import java.util.List;

/**
 * @author nadavy
 */
@Data
public class HelmMetadataInfo {

    private HelmInfo helmInfo;
    private List<HelmDependencyMetadataInfo> helmDependencies;

    HelmMetadataInfo(HelmInfo helmInfo, List<HelmDependencyMetadataInfo> helmDependencies) {
        this.helmInfo = helmInfo;
        this.helmDependencies = helmDependencies;
    }
}

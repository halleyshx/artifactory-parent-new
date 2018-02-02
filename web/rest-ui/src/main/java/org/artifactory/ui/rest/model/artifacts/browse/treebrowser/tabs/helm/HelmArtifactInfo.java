package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.helm;

import org.artifactory.addon.helm.HelmDependencyMetadataInfo;
import org.artifactory.addon.helm.HelmInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;

import java.util.List;

/**
 * @author nadavy
 */
public class HelmArtifactInfo extends BaseArtifactInfo {

    private HelmInfo helmInfo;
    private List<HelmDependencyMetadataInfo> helmDependencies;

    @SuppressWarnings({"UnusedDeclaration"})
    public HelmInfo getHelmInfo() {
        return helmInfo;
    }

    public void setHelmInfo(HelmInfo helmInfo) {
        this.helmInfo = helmInfo;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public List<HelmDependencyMetadataInfo> getHelmDependencies() {
        return helmDependencies;
    }

    public void setHelmDependencies(List<HelmDependencyMetadataInfo> helmDependencies) {
        this.helmDependencies = helmDependencies;
    }
}

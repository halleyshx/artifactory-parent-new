package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.debian;

import org.artifactory.addon.debian.DebianInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;

import java.util.List;

/**
 * The UI model for Debian and Opkg info tab
 * If a package is invalid and was not indexed we hold the indexFailureReason, otherwise- null
 * A message appears in the Info tab in case this variable is not null
 *
 * @author Yuval Reches
 */
public class DebianArtifactMetadataInfo extends BaseArtifactInfo {

    private DebianInfo debianInfo;
    private List<String> debianDependencies;
    private String indexFailureReason;

    public DebianInfo getDebianInfo() {
        return debianInfo;
    }

    public void setDebianInfo(DebianInfo debianInfo) {
        this.debianInfo = debianInfo;
    }

    public List<String> getDebianDependencies() {
        return debianDependencies;
    }

    public void setDebianDependencies(List<String> debianDependencies) {
        this.debianDependencies = debianDependencies;
    }

    public String getIndexFailureReason() {
        return indexFailureReason;
    }

    public void setIndexFailureReason(String indexFailureReason) {
        this.indexFailureReason = indexFailureReason;
    }
}

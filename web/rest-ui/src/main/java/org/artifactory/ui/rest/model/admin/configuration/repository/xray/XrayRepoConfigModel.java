package org.artifactory.ui.rest.model.admin.configuration.repository.xray;

import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_XRAY_BLOCK_UNSCANNED;
import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_XRAY_INDEX;

/**
 * @author Dan Feldman
 */
public class XrayRepoConfigModel implements RestModel {

    private boolean enabled = DEFAULT_XRAY_INDEX;
    private String minimumBlockedSeverity;
    private boolean blockUnscannedArtifacts = DEFAULT_XRAY_BLOCK_UNSCANNED;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMinimumBlockedSeverity() {
        return minimumBlockedSeverity;
    }

    public void setMinimumBlockedSeverity(String minimumBlockedSeverity) {
        this.minimumBlockedSeverity = minimumBlockedSeverity;
    }

    public boolean isBlockUnscannedArtifacts() {
        return blockUnscannedArtifacts;
    }

    public void setBlockUnscannedArtifacts(boolean blockUnscannedArtifacts) {
        this.blockUnscannedArtifacts = blockUnscannedArtifacts;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

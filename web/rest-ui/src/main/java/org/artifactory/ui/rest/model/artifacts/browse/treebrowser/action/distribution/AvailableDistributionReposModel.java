package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.distribution;

import com.google.common.collect.Lists;
import org.artifactory.repo.RepoPath;

import java.util.List;

/**
 * @author nadavy
 */
public class AvailableDistributionReposModel {
    private boolean offlineMode;
    private boolean distributionRepoConfigured;
    private List<RepoPath> availableDistributionRepos;

    public AvailableDistributionReposModel(List<RepoPath> availableDistributionRepos) {
        this.availableDistributionRepos = availableDistributionRepos;
        this.distributionRepoConfigured = true;
    }

    public AvailableDistributionReposModel(boolean offlineMode, boolean distributionRepoConfigured) {
        this.offlineMode = offlineMode;
        this.distributionRepoConfigured = distributionRepoConfigured;
        availableDistributionRepos = Lists.newArrayList();
    }

    public List<RepoPath> getAvailableDistributionRepos() {
        return availableDistributionRepos;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public boolean isDistributionRepoConfigured() {
        return distributionRepoConfigured;
    }
}

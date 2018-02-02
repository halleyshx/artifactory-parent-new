package org.artifactory.repo.onboarding;

import java.util.List;

/**
 * @author nadavy
 */
public class VirtualDefaultRepoModel {
    private String repoKey;
    private String defaultDeployment;
    private List<String> includedLocalRepos;
    private List<String> includedRemoteRepos;

    public VirtualDefaultRepoModel(String repoKey, String defaultDeployment,
            List<String> includedLocalRepos, List<String> includedRemoteRepos) {
        this.repoKey = repoKey;
        this.defaultDeployment = defaultDeployment;
        this.includedLocalRepos = includedLocalRepos;
        this.includedRemoteRepos = includedRemoteRepos;
    }

    public VirtualDefaultRepoModel() {
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getDefaultDeployment() {
        return defaultDeployment;
    }

    public void setDefaultDeployment(String defaultDeployment) {
        this.defaultDeployment = defaultDeployment;
    }

    public List<String> getIncludedLocalRepos() {
        return includedLocalRepos;
    }

    public void setIncludedLocalRepos(List<String> includedLocalRepos) {
        this.includedLocalRepos = includedLocalRepos;
    }

    public List<String> getIncludedRemoteRepos() {
        return includedRemoteRepos;
    }

    public void setIncludedRemoteRepos(List<String> includedRemoteRepos) {
        this.includedRemoteRepos = includedRemoteRepos;
    }
}

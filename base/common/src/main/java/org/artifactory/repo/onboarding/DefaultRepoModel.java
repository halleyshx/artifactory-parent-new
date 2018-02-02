package org.artifactory.repo.onboarding;

import java.util.List;

/**
 * Default repos per repo type model
 * populated from defaultRepository.json
 * in DefaultRepoCreatorService
 *
 * @author nadavy
 */
public class DefaultRepoModel {

    private String repoType;
    private List<String> localRepoKeys;
    private List<RemoteDefaultRepoModel> remoteRepoKeys;
    private List<VirtualDefaultRepoModel> virtualRepoKeys;
    private String layout;

    public DefaultRepoModel() {
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) {
        this.repoType = repoType;
    }

    public List<String> getLocalRepoKeys() {
        return localRepoKeys;
    }

    public void setLocalRepoKeys(List<String> localRepoKeys) {
        this.localRepoKeys = localRepoKeys;
    }

    public List<RemoteDefaultRepoModel> getRemoteRepoKeys() {
        return remoteRepoKeys;
    }

    public void setRemoteRepoKeys(List<RemoteDefaultRepoModel> remoteRepoKeys) {
        this.remoteRepoKeys = remoteRepoKeys;
    }

    public List<VirtualDefaultRepoModel> getVirtualRepoKeys() {
        return virtualRepoKeys;
    }

    public void setVirtualRepoKeys(List<VirtualDefaultRepoModel> virtualRepoKeys) {
        this.virtualRepoKeys = virtualRepoKeys;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

}

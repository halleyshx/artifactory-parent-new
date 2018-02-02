package org.artifactory.ui.rest.service.onboarding;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoType;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.List;

/**
 * Response model for default repositories created using REST UI
 *
 * @author nadavy
 */
public class CreateDefaultReposResponseModel {

    private List<CreatedReposByType> createdReposByTypes;

    @JsonIgnore
    private boolean valid = true;

    CreateDefaultReposResponseModel() {
    }

    public List<CreatedReposByType> getCreatedReposByTypes() {
        return createdReposByTypes;
    }

    void setCreatedReposByTypes(List<CreatedReposByType> createdReposByTypes) {
        this.createdReposByTypes = createdReposByTypes;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * adds an empty model to the wrapper repo list for desired repo type
     */
    public CreatedReposByType addRepoType(RepoType repoType) {
        if (createdReposByTypes == null) {
            createdReposByTypes = Lists.newArrayList();
        }
        CreatedReposByType reposByType = new CreatedReposByType(repoType);
        createdReposByTypes.add(reposByType);
        return reposByType;
    }

    public static class CreatedReposByType {

        private String type;
        private List<String> localRepos;
        private List<String> remoteRepos;
        private List<String> virtualRepos;

        CreatedReposByType(RepoType repoType) {
            this.type = repoType.getType();
        }

        public CreatedReposByType() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getLocalRepos() {
            return localRepos;
        }

        public void setLocalRepos(List<String> localRepos) {
            this.localRepos = localRepos;
        }

        public List<String> getRemoteRepos() {
            return remoteRepos;
        }

        public void setRemoteRepos(List<String> remoteRepos) {
            this.remoteRepos = remoteRepos;
        }

        public List<String> getVirtualRepos() {
            return virtualRepos;
        }

        public void setVirtualRepos(List<String> virtualRepos) {
            this.virtualRepos = virtualRepos;
        }
    }
}

package org.artifactory.repo.onboarding;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author nadavy
 */
public class RemoteDefaultRepoModel {
    private String repoKey;

    @JsonProperty
    private String url;

    public RemoteDefaultRepoModel() {
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

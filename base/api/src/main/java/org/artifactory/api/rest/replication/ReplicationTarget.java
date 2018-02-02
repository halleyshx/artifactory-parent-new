package org.artifactory.api.rest.replication;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.jfrog.client.util.PathUtils;

public class ReplicationTarget {
    private String url;
    private String repoKey;
    private ReplicationStatusType status;
    private String lastCompleted;

    public ReplicationTarget(String fullRepoPath, ReplicationStatus status) {
        this.repoKey = PathUtils.getLastPathElement(fullRepoPath);
        this.url = fullRepoPath;
        this.status = status.getType();
        this.lastCompleted = status.getLastCompleted();
    }

    public String getUrl() {
        return url;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public String getStatus() {
        return status.getId();
    }

    public String getLastCompleted() {
        return lastCompleted;
    }

    @JsonIgnore
    public ReplicationStatusType getType() {
        return status;
    }

    public void setLastCompleted(String lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public void setStatus(String status) {
        this.status = ReplicationStatusType.findTypeById(status);;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }


    @JsonIgnore
    public Boolean replicationStatusByFullRepoPathExists(String fullRepoPath) {
        if (this.url.equals(fullRepoPath)){
            return true;
        }
        return false;
    }
}

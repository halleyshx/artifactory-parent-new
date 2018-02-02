package org.artifactory.addon.replication;

/**
 * @author Dan Feldman
 */
public class ReplicationTargetInfo {

    final public String instanceUrl;
    final public String repoKey;

    public ReplicationTargetInfo(String instanceUrl, String repoKey) {
        this.instanceUrl = instanceUrl;
        this.repoKey = repoKey;
    }

    @Override
    public String toString() {
        return instanceUrl + "/" + repoKey;
    }
}

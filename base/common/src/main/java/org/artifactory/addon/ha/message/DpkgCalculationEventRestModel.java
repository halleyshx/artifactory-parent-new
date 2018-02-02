package org.artifactory.addon.ha.message;

/**
 * Model for propagation, similar to DpkgCalculationEvent, but should only be used for propagation and should not
 * be passed to the actual index
 *
 * @author Shay Bagants
 */
public abstract class DpkgCalculationEventRestModel {

    private String repoKey;
    private String passphrase = null;
    private long timestamp;
    private boolean isIndexEntireRepo;

    public DpkgCalculationEventRestModel() {
    }

    public DpkgCalculationEventRestModel(String repoKey, String passphrase, long timestamp, boolean isIndexEntireRepo) {
        this.repoKey = repoKey;
        this.passphrase = passphrase;
        this.timestamp = timestamp;
        this.isIndexEntireRepo = isIndexEntireRepo;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isIndexEntireRepo() {
        return isIndexEntireRepo;
    }

    public void setIndexEntireRepo(boolean indexEntireRepo) {
        isIndexEntireRepo = indexEntireRepo;
    }
}

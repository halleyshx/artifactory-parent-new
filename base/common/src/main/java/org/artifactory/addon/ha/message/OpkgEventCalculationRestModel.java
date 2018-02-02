package org.artifactory.addon.ha.message;

import org.artifactory.addon.opkg.OpkgCalculationEvent;

/**
 * Model for propagation, similar to OpkgCalculationEvent, but should only be used for propagation and should not be
 * used as a part of the actual indexing process
 *
 * @author Shay Bagants
 */
public class OpkgEventCalculationRestModel extends DpkgCalculationEventRestModel {

    private String path;

    public OpkgEventCalculationRestModel() {
        super();
    }

    public OpkgEventCalculationRestModel(String repoKey, String passphrase, long timestamp, boolean isIndexEntireRepo) {
        super(repoKey, passphrase, timestamp, isIndexEntireRepo);
    }

    public OpkgEventCalculationRestModel(OpkgCalculationEvent event) {
        super(event.getRepoKey(), event.getPassphrase(), event.getTimestamp(), event.isIndexEntireRepo());
        this.path = event.getPath();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

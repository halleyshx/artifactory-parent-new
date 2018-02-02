package org.artifactory.addon.replication.event;

/**
 * Interface for event based replication listeners that are handling replication channels
 *
 * @author nadavy
 */
public interface ReplicationChannelListener {

    ReplicationChannel establishReplicationChannel(ReplicationOwnerModel replicationOwnerModel, String targetNode);

    void removeReplicationChannel(ReplicationOwnerModel replicationOwnerModel);
}

package org.artifactory.addon.replication.event;


import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * @author nadavy
 */
@JsonDeserialize(as=RemoteReplicationEventQueueItem.class)
public interface ReplicationEventQueueItem{

    String getRepoKey();

    String getPath();

    EventType getEventType();

}

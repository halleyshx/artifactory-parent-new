package org.artifactory.addon.replication.event;

/**
 * @author nadavy
 */
public class RemoteReplicationEventQueueItem implements ReplicationEventQueueItem {

    private String repoKey;
    private String path;
    private EventType eventType;

    public static RemoteReplicationEventQueueItem EMPTY = new RemoteReplicationEventQueueItem("", "", EventType.EMPTY);

    public RemoteReplicationEventQueueItem() {
    }

    public RemoteReplicationEventQueueItem(String repoKey, String path, EventType eventType) {
        this.repoKey = repoKey;
        this.path = path;
        this.eventType = eventType;
    }

    public RemoteReplicationEventQueueItem(ReplicationEventQueueItem event) {
        this.repoKey = event.getRepoKey();
        this.path = event.getPath();
        this.eventType = event.getEventType();
    }

    @Override
    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "RemoteReplicationEventQueueItem{" +
                ", path='" + path + '\'' +
                ", eventType=" + eventType +
                '}';
    }
}

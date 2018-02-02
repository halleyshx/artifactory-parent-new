package org.artifactory.addon.replication.event;

import org.artifactory.descriptor.config.CentralConfigDescriptor;

/**
 * Interface representing the flow that delivers events from event based replication publishers to the subscribers.
 *
 * @author Dan Feldman
 */
public interface ReplicationChannel {

    void handleReplicationEvents(ReplicationEventQueueWorkItem event);

    void destroy();

    void reload(CentralConfigDescriptor newDescriptor, CentralConfigDescriptor oldDescriptor);

    ChannelType getChannelType();

    String getOwner();
}

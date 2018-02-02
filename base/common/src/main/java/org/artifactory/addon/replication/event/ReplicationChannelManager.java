package org.artifactory.addon.replication.event;

import org.artifactory.descriptor.config.CentralConfigDescriptor;

import java.util.List;

/**
 * Interface for the event based replication inbound and outbound managers
 *
 * @author Nadav Yogev
 */
public interface ReplicationChannelManager extends ReplicationChannelListener {

    void handleReplicationEvents(ReplicationEventQueueWorkItem event);

    /**
     * Handles events from another node in the cluster
     */
    void handlePropagatedRemoteReplicationEvents(ReplicationEventQueueWorkItem events);

    void reload(CentralConfigDescriptor newDescriptor, CentralConfigDescriptor oldDescriptor);

    /**
     * Closes all channels - clustered or streamed, of the manager
     */
    void destroy();

    /**
     * Returns a list of repo keys that have a channel of a given channel type
     */
    List<String> getNodeChannels(ChannelType channelType);

    /**
     * Returns true if there are any replication channels for a given repo key
     */
    boolean hasRemoteReplicationForRepo(String repoKey);
}

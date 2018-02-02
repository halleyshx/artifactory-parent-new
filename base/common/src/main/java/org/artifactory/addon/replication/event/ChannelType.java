package org.artifactory.addon.replication.event;

/**
 * Event based pull replication channel types
 *
 * @author nadavy
 */
public enum ChannelType {
    inboundStream,   // Channel that receives events from a remote repository and replicate them
    inboundCluster,  // HA node channels that isn't the owner of the replication
    outboundStream,  // channel that sends local binary changes to a subscribed artifactory server
    outboundCluster; // HA node channels that propagate events to an outboundStream channel

    public boolean isClusterChannel(){
        return this == inboundCluster || this == outboundCluster;
    }

    public boolean isOutboundStream() {
        return this == outboundStream;
    }
}

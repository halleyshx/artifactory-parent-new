package org.artifactory.addon.replication.event;

/**
 * Model for creating/deleting event based replication channels
 *
 * @author nadavy
 */
public class ReplicationOwnerModel {

    private String repoKey;
    private String owner;
    private String channelUuid;
    private String channelType;

    public ReplicationOwnerModel() {
    }

    /**
     * Constructor for cluster channels
     */
    public ReplicationOwnerModel(String repoKey, String owner, String channelUuid, String channelType) {
        this.repoKey = repoKey;
        this.owner = owner;
        this.channelUuid = channelUuid;
        this.channelType = channelType;
    }

    /**
     * Constructor for creating streamed channels
     */
    public ReplicationOwnerModel(String repoKey, String owner, String channelType) {
        this.repoKey = repoKey;
        this.owner = owner;
        this.channelType = channelType;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getChannelUuid() {
        return channelUuid;
    }

    public void setChannelUuid(String channelUuid) {
        this.channelUuid = channelUuid;
    }

    public ChannelType retrieveChannelType() {
        return ChannelType.valueOf(channelType);
    }
}

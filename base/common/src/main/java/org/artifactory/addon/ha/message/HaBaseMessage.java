package org.artifactory.addon.ha.message;

import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Shay Bagants
 */
@JsonTypeName("haBaseEvent")
public class HaBaseMessage implements HaMessage {

    private String publishingMemberId;

    public HaBaseMessage() {
    }

    public HaBaseMessage(String publishingMemberId) {
        this.publishingMemberId = publishingMemberId;
    }

    @Override
    public String getPublishingMemberId() {
        return publishingMemberId;
    }

    @Override
    public void setPublishingMemberId(String publishingMemberId) {
        this.publishingMemberId = publishingMemberId;
    }
}

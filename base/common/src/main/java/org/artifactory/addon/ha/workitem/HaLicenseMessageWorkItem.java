package org.artifactory.addon.ha.workitem;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;

import javax.annotation.Nonnull;

/**
 * @author Shay Bagants
 */
public class HaLicenseMessageWorkItem implements HaMessageWorkItem {

    private String topicName = HaMessageTopic.LICENSES_CHANGE_TOPIC.topicName();
    private HaMessage haMessage;

    public HaLicenseMessageWorkItem(HaMessage haMessage) {
        this.haMessage = haMessage;
    }

    @Override
    public HaMessage getMessage() {
        return haMessage;
    }

    @Override
    public HaMessageTopic getTopic() {
        return HaMessageTopic.LICENSES_CHANGE_TOPIC;
    }

    // equals and hashcode are always the topic name because we want to aggregate the reloadLicense requests and do it once.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HaLicenseMessageWorkItem that = (HaLicenseMessageWorkItem) o;

        return topicName != null ? topicName.equals(that.topicName) : that.topicName == null;
    }

    @Override
    public int hashCode() {
        return topicName != null ? topicName.hashCode() : 0;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        return topicName;
    }
}

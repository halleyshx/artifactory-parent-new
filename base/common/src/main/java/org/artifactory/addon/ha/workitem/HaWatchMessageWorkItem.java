package org.artifactory.addon.ha.workitem;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;

import javax.annotation.Nonnull;

/**
 * @author Shay Bagants
 */
public class HaWatchMessageWorkItem implements HaMessageWorkItem {

    String topicName = HaMessageTopic.WATCHES_TOPIC.topicName();
    private HaMessage haMessage;

    public HaWatchMessageWorkItem(HaMessage haMessage) {
        this.haMessage = haMessage;
    }

    @Override
    public HaMessage getMessage() {
        return haMessage;
    }

    @Override
    public HaMessageTopic getTopic() {
        return HaMessageTopic.WATCHES_TOPIC;
    }

    // equals and hashcode are not the same as getUniqueKey. Do not change this. See HaMessageWorkItem javadoc for more information
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HaWatchMessageWorkItem that = (HaWatchMessageWorkItem) o;

        if (topicName != null ? !topicName.equals(that.topicName) : that.topicName != null) {
            return false;
        }
        return haMessage != null ? haMessage.equals(that.haMessage) : that.haMessage == null;
    }

    @Override
    public int hashCode() {
        int result = topicName != null ? topicName.hashCode() : 0;
        result = 31 * result + (haMessage != null ? haMessage.hashCode() : 0);
        return result;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        return topicName;
    }
}

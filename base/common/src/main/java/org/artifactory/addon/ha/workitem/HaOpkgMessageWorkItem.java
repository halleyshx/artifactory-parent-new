package org.artifactory.addon.ha.workitem;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;

import javax.annotation.Nonnull;

/**
 * @author Shay Bagants
 */
public class HaOpkgMessageWorkItem implements HaMessageWorkItem {

    private String topicName = HaMessageTopic.CALCULATE_OPKG.topicName();
    private HaMessage haMessage;
    private final String uniqueKey;


    public HaOpkgMessageWorkItem(HaMessage haMessage) {
        this.haMessage = haMessage;
        this.uniqueKey = getClass().getName() + ":" + System.currentTimeMillis() + System.nanoTime() + ":" + Math.random();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HaOpkgMessageWorkItem that = (HaOpkgMessageWorkItem) o;

        if (topicName != null ? !topicName.equals(that.topicName) : that.topicName != null) {
            return false;
        }
        return uniqueKey != null ? uniqueKey.equals(that.uniqueKey) : that.uniqueKey == null;
    }

    @Override
    public int hashCode() {
        int result = topicName != null ? topicName.hashCode() : 0;
        result = 31 * result + (uniqueKey != null ? uniqueKey.hashCode() : 0);
        return result;
    }

    @Override
    public HaMessage getMessage() {
        return haMessage;
    }

    @Override
    public HaMessageTopic getTopic() {
        return HaMessageTopic.CALCULATE_OPKG;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        return topicName;
    }
}

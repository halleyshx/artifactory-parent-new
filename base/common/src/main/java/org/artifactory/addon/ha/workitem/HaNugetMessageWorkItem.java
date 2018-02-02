package org.artifactory.addon.ha.workitem;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;

import javax.annotation.Nonnull;

/**
 * @author Shay Bagants
 */
public class HaNugetMessageWorkItem implements HaMessageWorkItem {

    private String topicName = HaMessageTopic.NUPKG_TOPIC.topicName();
    private HaMessage haMessage;

    public HaNugetMessageWorkItem(HaMessage haMessage) {
        this.haMessage = haMessage;
    }

    @Override
    public HaMessage getMessage() {
        return haMessage;
    }

    @Override
    public HaMessageTopic getTopic() {
        return HaMessageTopic.NUPKG_TOPIC;
    }

    // we compare the ha message itself. The equals and hashcode matches, however, they are not the same as uniqueKey,
    // this is because we want to aggregate the nuget events so only single thread will work on the same type of events,
    // but we don't want that the workQueue will think that multiple events are the same files and will clean them
    // from the queue because it will think that these are duplications. See HaMessageWorkItem javadoc
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HaNugetMessageWorkItem that = (HaNugetMessageWorkItem) o;

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

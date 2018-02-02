package org.artifactory.addon.ha.workitem;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;
import org.artifactory.api.repo.WorkItem;

/**
 * Ha message interface. Unlike regular work item, some of the implementations will have equals implementation which is
 * **DIFFERENT THAN** the unique key implementation. The reason for this, is because the workqueue takes a lock per
 * uniqueKey, meaning that only one worker will work on the same unique key (e.g. aclEvent, watchEvent, nugetEvent etc..),
 * however, for some events, we want one worker to work on it but we don't want to remove "duplicates", because these
 * are not real duplicates.
 * For example, we can have multiple watch events and when working on the first event in the queue, we want the other
 * events to be left in the queue and not be deleted, while a single worker is fetching them one by one and execute them
 * in the ORIGINAL order.
 *
 * @author Shay Bagants
 */
public interface HaMessageWorkItem extends WorkItem {

    HaMessage getMessage();

    HaMessageTopic getTopic();
}

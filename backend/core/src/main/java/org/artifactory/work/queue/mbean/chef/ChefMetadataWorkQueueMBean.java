package org.artifactory.work.queue.mbean.chef;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author Alexis Tual
 */
public interface ChefMetadataWorkQueueMBean extends WorkQueueMBean {
    int getQueueSize();
    int getNumberOfWorkers();
    int getMaxNumberOfWorkers();
    String getName();
}

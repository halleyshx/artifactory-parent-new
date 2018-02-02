package org.artifactory.work.queue.mbean.chef;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 *
 * Working queue handling reindexing work items.
 *
 * @author Alexis Tual
 */
public class ChefMetadataWorkQueue implements ChefMetadataWorkQueueMBean {

    private WorkQueueMBean workQueueMBean;

    public ChefMetadataWorkQueue(WorkQueueMBean workQueueMBean) {
        this.workQueueMBean = workQueueMBean;
    }

    @Override
    public int getQueueSize() {
        return workQueueMBean.getQueueSize();
    }

    @Override
    public int getNumberOfWorkers() {
        return workQueueMBean.getNumberOfWorkers();
    }

    @Override
    public int getMaxNumberOfWorkers() {
        return workQueueMBean.getMaxNumberOfWorkers();
    }

    @Override
    public String getName() {
        return workQueueMBean.getName();
    }

}

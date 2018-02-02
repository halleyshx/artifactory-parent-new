package org.artifactory.work.queue.mbean.buildEvent;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author Liza Dashevski
 */
public class BuildRetentionWorkQueue implements BuildRetentionWorkQueueMBean {
    private WorkQueueMBean workQueueMBean;


    public BuildRetentionWorkQueue(WorkQueueMBean workQueueMBean) {
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
package org.artifactory.work.queue.mbean.yum;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author Yuval Reches
 */
public class RpmWorkQueue implements RpmWorkQueueMBean {
    private WorkQueueMBean workQueueMBean;

    public RpmWorkQueue(WorkQueueMBean workQueueMBean) {
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

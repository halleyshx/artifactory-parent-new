package org.artifactory.work.queue.mbean.replication;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author nadavy
 */
public class ReplicationEventQueue implements ReplicationEventQueueMBean {

    private WorkQueueMBean workQueueMBean;

    public ReplicationEventQueue(WorkQueueMBean workQueueMBean) {
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

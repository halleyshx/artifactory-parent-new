package org.artifactory.work.queue.mbean.ha;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * Queue for Artifactory event messages (messages that used to be sent using hazelcast)
 *
 * @author Shay Bagants
 */
public class HaMessageWorkQueue implements HaMessageWorkQueueMBean {
    private WorkQueueMBean workQueueMBean;

    public HaMessageWorkQueue(WorkQueueMBean workQueueMBean) {
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

package org.artifactory.work.queue.mbean.repo.path.checksum;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author gidis
 */
public class RepoPathChecksumCalculationJobWorkQueue implements RepoPathChecksumCalculationJobWorkQueueMBean{
    private WorkQueueMBean workQueueMBean;

    public RepoPathChecksumCalculationJobWorkQueue(WorkQueueMBean workQueueMBean) {
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

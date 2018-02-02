/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.schedule.aop;

import org.artifactory.api.repo.WorkItem;
import org.artifactory.api.repo.WorkQueue;

/**
 * @author gidis
 */
class WorkExecution {
    final WorkQueue<WorkItem> workQueue;
    final WorkItem workItem;
    final boolean blockUntilFinished; //Signifies a thread is polling on this work item to finish.

    WorkExecution(WorkQueue<WorkItem> workQueue, WorkItem workItem, boolean blockUntilFinished) {
        this.workQueue = workQueue;
        this.workItem = workItem;
        this.blockUntilFinished = blockUntilFinished;
    }
}

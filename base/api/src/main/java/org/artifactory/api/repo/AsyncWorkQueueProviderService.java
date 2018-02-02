/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.repo;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author gidis
 */
public interface AsyncWorkQueueProviderService {

    /**
     * Get a work queue. If no work queue exists yet, create one.
     */
    WorkQueue<WorkItem> getWorkQueue(Method workQueueCallback, Object target);

    /**
     * Get all existing work queues.
     * @return all existing work queues.
     */
    Map<String, WorkQueue<WorkItem>> getExistingWorkQueues();

    void closeAllQueues();

    int getEstimatedPendingTasksSize(String workQueueCallbackName);
}
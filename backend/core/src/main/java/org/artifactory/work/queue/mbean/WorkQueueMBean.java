/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.work.queue.mbean;

/**
 * @author gidis
 */
public interface WorkQueueMBean {

    int getQueueSize();
    int getNumberOfWorkers();
    int getMaxNumberOfWorkers();
    String getName();
}

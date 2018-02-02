/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.work.queue.mbean.maven;

import org.artifactory.work.queue.mbean.WorkQueueMBean;

/**
 * @author gidis
 */
public interface MavenPluginMetadataWorkQueueMBean extends WorkQueueMBean {
    int getQueueSize();
    int getNumberOfWorkers();
    int getMaxNumberOfWorkers();
    String getName();
}

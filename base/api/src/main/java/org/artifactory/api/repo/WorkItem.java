/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.repo;

import javax.annotation.Nonnull;

/**
 * @author gidis
 */
public interface WorkItem {

    /**
     * This key will be used by the locking map that synchronizes all work items
     * - it **MUST** match the equals() and hashcode() relations of this work item.
     */
    @Nonnull
    String getUniqueKey();
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.listeners;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;

/**
 * @author gidis
 */
public interface MoveCopyListeners {
    void notifyAfterMoveCopy(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context);
    void notifyBeforeMoveCopy(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context);
    boolean isInterested(MoveCopyItemInfo element, MoveCopyContext context);

}

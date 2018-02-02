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
public class MavenMetaDataMoveCopyListener implements MoveCopyListeners {
    @Override
    public void notifyAfterMoveCopy(MoveCopyItemInfo itemInfo, MoveMultiStatusHolder status, MoveCopyContext context) {

    }

    @Override
    public void notifyBeforeMoveCopy(MoveCopyItemInfo itemInfo, MoveMultiStatusHolder status, MoveCopyContext context) {

    }

    @Override
    public boolean isInterested(MoveCopyItemInfo itemInfo, MoveCopyContext context) {
        return false;
    }
}

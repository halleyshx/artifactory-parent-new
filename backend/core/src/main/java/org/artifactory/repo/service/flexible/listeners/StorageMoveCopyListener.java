/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.listeners;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;
import org.artifactory.sapi.fs.VfsFolder;

/**
 * @author gidis
 */
public class StorageMoveCopyListener implements MoveCopyListeners {
    private StorageInterceptors storageInterceptors;

    public StorageMoveCopyListener(StorageInterceptors storageInterceptors) {
        this.storageInterceptors = storageInterceptors;
    }

    @Override
    public void notifyAfterMoveCopy(MoveCopyItemInfo itemInfo, MoveMultiStatusHolder status, MoveCopyContext context) {
        if (!context.isDryRun()) {
            if (itemInfo.getMutableTargetItem().isFile()) {
                if (context.isCopy()) {
                    storageInterceptors.afterCopy(itemInfo.getSourceItem(), itemInfo.getMutableTargetItem(), status, new PropertiesImpl());
                } else {
                    storageInterceptors.afterMove(itemInfo.getSourceItem(), itemInfo.getMutableTargetItem(), status, new PropertiesImpl());
                }
            } else {
                if (shouldRemoveSourceFolder((VfsFolder) itemInfo.getSourceItem(), context, status)) {
                    if (context.isCopy()) {
                        storageInterceptors.afterCopy(itemInfo.getSourceItem(), itemInfo.getMutableTargetItem(), status, new PropertiesImpl());
                    } else {
                        storageInterceptors.afterMove(itemInfo.getSourceItem(), itemInfo.getMutableTargetItem(), status, new PropertiesImpl());
                    }
                }
            }
        }
    }

    /**
     * If not in a dry run, If not pruning empty folders (if true it will happen at a later stage),
     * If not copying (no source removal when copying), If not on the root item (a repo),
     * If not containing any children and folders or artifacts were moved.
     */
    protected boolean shouldRemoveSourceFolder(VfsFolder sourceFolder, MoveCopyContext context, MoveMultiStatusHolder status) {
        return !context.isDryRun() && !context.isCopy() && !sourceFolder.getRepoPath().isRoot() && !sourceFolder.hasChildren()
                && !context.isPruneEmptyFolders() && (status.getMovedFoldersCount() != 0 || status.getMovedArtifactsCount() != 0);
    }

    @Override
    public void notifyBeforeMoveCopy(MoveCopyItemInfo itemInfo, MoveMultiStatusHolder status, MoveCopyContext context) {
        if (context.isCopy()) {
            storageInterceptors.beforeCopy(itemInfo.getSourceItem(),
                    itemInfo.getTargetRepoPath(), status, itemInfo.getSourceItem().getProperties());
        } else {
            storageInterceptors.beforeMove(itemInfo.getSourceItem(), itemInfo.getTargetRepoPath(), status,
                    itemInfo.getSourceItem().getProperties());
        }
    }

    @Override
    public boolean isInterested(MoveCopyItemInfo itemInfo, MoveCopyContext context) {
        return true;
    }
}

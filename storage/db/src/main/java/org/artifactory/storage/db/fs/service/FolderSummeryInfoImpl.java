package org.artifactory.storage.db.fs.service;

import org.artifactory.api.repo.storage.FolderSummeryInfo;

public class FolderSummeryInfoImpl implements FolderSummeryInfo {

    private final long folderSize;
    private final long fileCount;

    public FolderSummeryInfoImpl(long fileCount, long folderSize) {
        this.fileCount = fileCount;
        this.folderSize = folderSize;
    }


    public long getFolderSize() {
        return folderSize;
    }

    public long getFileCount() {
        return fileCount;
    }
}

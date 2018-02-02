package org.artifactory.storage.db.fs.entity;

/**
 * @author Liza Dashevski
 */
public class FolderSummeryNodeInfoImpl implements FolderSummeryNodeInfo {

    private final long folderSize;
    private final long filesCount;

    public FolderSummeryNodeInfoImpl(long filesCount, long folderSize) {
        this.folderSize = folderSize;
        this.filesCount = filesCount;
    }

    public long getFolderSize() {
        return folderSize;
    }

    public long getFileCount() {
        return filesCount;
    }
}

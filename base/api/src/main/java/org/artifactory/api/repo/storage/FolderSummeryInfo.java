package org.artifactory.api.repo.storage;

/**
 * @author Liza Dashevski
 */
public interface FolderSummeryInfo {

    long getFolderSize();

    long getFileCount();
}

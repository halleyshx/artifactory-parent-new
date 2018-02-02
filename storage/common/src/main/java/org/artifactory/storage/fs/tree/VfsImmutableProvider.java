package org.artifactory.storage.fs.tree;

import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;

/**
 * @author gidis
 */
public interface VfsImmutableProvider {
    VfsItem getImmutableFsItem();

    VfsFile getImmutableFile();

    VfsFolder getImmutableFolder();
}

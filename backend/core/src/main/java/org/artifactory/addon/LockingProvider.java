package org.artifactory.addon;

import org.artifactory.storage.fs.lock.FsItemsVault;
import org.jfrog.storage.common.ConflictGuard;
import org.jfrog.storage.common.ConflictsGuard;

/**
 * @author gidis
 */
public interface LockingProvider {

    FsItemsVault getFolderLockingMap();

    FsItemsVault getFileLockingMap();

    ConflictGuard getConflictGuard(String key);

    <T>ConflictsGuard<T> getConflictsGuard(String key);
}

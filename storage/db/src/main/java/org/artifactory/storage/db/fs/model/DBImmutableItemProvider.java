package org.artifactory.storage.db.fs.model;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.fs.VfsItemNotFoundException;
import org.artifactory.storage.fs.lock.FsItemLockEntry;
import org.artifactory.storage.fs.lock.LockingHelper;
import org.artifactory.storage.fs.repo.StoringRepo;
import org.artifactory.storage.fs.service.FileService;
import org.artifactory.storage.fs.session.StorageSessionHolder;
import org.artifactory.storage.fs.tree.VfsImmutableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * @author gidis
 */
public class DBImmutableItemProvider implements VfsImmutableProvider {
    private static final Logger log = LoggerFactory.getLogger(DBImmutableItemProvider.class);

    private final StoringRepo storingRepo;
    private final RepoPath repoPath;

    public DBImmutableItemProvider(StoringRepo storingRepo, RepoPath repoPath) {
        this.storingRepo = storingRepo;
        this.repoPath = repoPath;
        if (!storingRepo.getKey().equals(repoPath.getRepoKey())) {
            throw new IllegalArgumentException(
                    "Attempt to lock '" + repoPath + "' with repository '" + storingRepo.getKey());
        }
    }


    @Override
    @Nullable
    public VfsItem getImmutableFsItem() {
        return getImmutablefsItem();
    }

    @Override
    @Nullable
    public VfsFolder getImmutableFolder() {
        VfsItem immutableFsFolder = getImmutablefsItem();
        if (immutableFsFolder != null && immutableFsFolder.isFile()) {
            throw new FolderExpectedException(getRepoPath());
        }
        return (VfsFolder) immutableFsFolder;
    }

    @Override
    @Nullable
    public VfsFile getImmutableFile() {
        VfsItem immutableFsFile = getImmutablefsItem();
        if (immutableFsFile != null && immutableFsFile.isFolder()) {
            throw new FileExpectedException(getRepoPath());
        }
        return (VfsFile) immutableFsFile;
    }

    @Nullable
    private VfsItem fetchVfsItem(RepoPath repoPath) {
        FileService fileService = ContextHelper.get().beanForType(FileService.class);
        VfsItem item;
        try {
            item = fileService.loadVfsItem(storingRepo, repoPath);
        } catch (VfsItemNotFoundException e) {
            item = null;
        } catch (StorageException e) {
            throw new StorageException(e);
        }
        return item;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    /**
     * Returns an read locked immutable VFS item or null if the item with this repo path doesn't exist.
     * If the item already write locked by the current thread, no read lock is acquired.
     *
     * @return Read locked VFS item or null if not found
     */
    private <T extends VfsItem> T getImmutablefsItem() {
        if (StorageSessionHolder.getSession() != null) {
            // try first the mutable item - it might contain changes done by current thread in a previous method
            FsItemLockEntry sessionLockEntry = LockingHelper.getLockEntry(repoPath);
            if (sessionLockEntry != null) {
                if (sessionLockEntry.getMutableFsItem() != null) {
                    return (T) sessionLockEntry.getMutableFsItem();
                }
            }
        }

        // get the requested fs item from the storage
        return (T) fetchVfsItem(repoPath);
    }
}

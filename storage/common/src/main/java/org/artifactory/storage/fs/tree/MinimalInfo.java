package org.artifactory.storage.fs.tree;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.fs.service.FileService;

/**
 * @author gidis
 */
public enum MinimalInfo {
    file, folder, notExist;

    public static MinimalInfo resolve(RepoPath repoPath) {
        FileService fileService = ContextHelper.get().beanForType(FileService.class);
        Boolean result = fileService.isFile(repoPath);
        if (result == null) {
            return notExist;
        } else if (result) {
            return file;
        } else {
            return folder;
        }
    }

    public boolean isExist() {
        return this != notExist;
    }

    public boolean isFile() {
        return this == file;
    }

    public boolean isFolder() {
        return this == folder;
    }
}

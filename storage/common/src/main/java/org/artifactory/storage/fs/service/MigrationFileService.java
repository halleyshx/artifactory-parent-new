package org.artifactory.storage.fs.service;

import org.artifactory.repo.RepoPath;

/**
 * @author gidis
 */
@Deprecated
public interface MigrationFileService {
    void updateRepoPathChecksum(RepoPath repoPath);
}

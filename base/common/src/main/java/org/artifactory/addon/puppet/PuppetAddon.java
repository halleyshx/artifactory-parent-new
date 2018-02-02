package org.artifactory.addon.puppet;

import org.artifactory.addon.Addon;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;

/**
 * Created by jainishshah on 9/1/16.
 */
public interface PuppetAddon extends Addon{

    void addPuppetPackage(FileInfo repoPath);

    void removePuppetPackage(FileInfo fileInfo);

    void reindexPuppetRepo(String repoKey);

    @Nonnull
    PuppetInfo getPuppetInfo(RepoPath repoPath);

    boolean isPuppetFile(RepoPath repoPath);
}

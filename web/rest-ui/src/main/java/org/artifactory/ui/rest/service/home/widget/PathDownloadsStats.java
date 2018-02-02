package org.artifactory.ui.rest.service.home.widget;

import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Yinon Avraham.
 */
public class PathDownloadsStats {

    private final RepoPath repoPath;
    private final long downloads;

    public PathDownloadsStats(@Nonnull RepoPath repoPath, long downloads) {
        this.repoPath = Objects.requireNonNull(repoPath, "repoPath is required");
        this.downloads = downloads;
    }

    @Nonnull
    public RepoPath getRepoPath() {
        return repoPath;
    }

    public long getDownloads() {
        return downloads;
    }
}

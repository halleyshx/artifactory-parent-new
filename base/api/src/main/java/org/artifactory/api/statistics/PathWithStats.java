package org.artifactory.api.statistics;

import org.artifactory.fs.StatsInfo;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;

/**
 * @author Yinon Avraham.
 */
public interface PathWithStats {

    @Nonnull
    RepoPath getPath();

    @Nonnull
    StatsInfo getStats();
}

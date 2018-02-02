package org.artifactory.statistics;

import org.artifactory.api.statistics.PathWithStats;
import org.artifactory.fs.StatsInfo;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * @author Yinon Avraham.
 */
public class PathWithStatsImpl implements PathWithStats {

    private final RepoPath path;
    private final StatsInfo stats;

    public PathWithStatsImpl(RepoPath path, StatsInfo stats) {
        this.path = requireNonNull(path, "path is required");
        this.stats = requireNonNull(stats, "stats is required");
    }

    @Override
    @Nonnull
    public RepoPath getPath() {
        return path;
    }

    @Override
    @Nonnull
    public StatsInfo getStats() {
        return stats;
    }
}

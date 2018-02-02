package org.artifactory.api.statistics;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Yinon Avraham.
 */
public interface StatisticsService {

    /**
     * Get the top most downloaded paths.
     * @param limit the max number of results to return
     * @return an ordered list with the top most downloaded paths with their stats
     */
    @Nonnull
    List<PathWithStats> getMostDownloaded(int limit);

    /**
     * Flush and persist the stats currently collected only in memory.
     * <p>
     * <b>Use with care!!!</b>
     * </p>
     */
    void flushDownloadStatistics();
}

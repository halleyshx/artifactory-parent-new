/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.search.stats;

import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.stats.StatsSearchControls;
import org.artifactory.api.search.stats.StatsSearchResult;
import org.artifactory.aql.AqlConverts;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.search.AqlSearcherBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.artifactory.aql.api.internal.AqlBase.and;
import static org.artifactory.aql.api.internal.AqlBase.or;

/**
 * @author Noam Y. Tenne
 */
public class LastDownloadedSearcher extends AqlSearcherBase<StatsSearchControls, StatsSearchResult> {
    private static final Logger log = LoggerFactory.getLogger(LastDownloadedSearcher.class);

    @Override
    public ItemSearchResults<StatsSearchResult> doSearch(StatsSearchControls controls) {
        long since = controls.getDownloadedSince().getTimeInMillis();
        //If createdBefore is not specified will only return artifacts that were created before downloadedSince
        long createdBefore = since;
        if (controls.hasCreatedBefore()) {
            createdBefore = controls.getCreatedBefore().getTimeInMillis();
        }
        AqlBase.AndClause<AqlApiItem> query = and();
        AqlBase.OrClause<AqlApiItem> reposToSearchFilter = getSelectedReposForSearchClause(controls);
        if (!reposToSearchFilter.isEmpty()) {
            log.debug("Filtering not used since search by repos: {}", controls.getSelectedRepoForSearch());
            query.append(reposToSearchFilter);
        }
        query.append(
                or(
                        AqlApiItem.statistic().downloaded().less(since),
                        AqlApiItem.statistic().downloaded().equals(null)
                )
        );
        query.append(
                or(
                        AqlApiItem.statistic().remoteDownloaded().less(since),
                        AqlApiItem.statistic().remoteDownloaded().equals(null)
                )
        );
        query.append(AqlApiItem.created().less(createdBefore));
        AqlEagerResult<AqlItem> result = executeQuery(query, controls);
        return new ItemSearchResults<>(collectStatsSearchResults(result), result.getSize());
    }

    private List<StatsSearchResult> collectStatsSearchResults(AqlEagerResult<AqlItem> result) {
        return result.getResults().stream()
                .map(AqlConverts.toItemInfo)
                .filter(item -> {
                    LocalRepo repo = getRepoService().localOrCachedRepositoryByKey(item.getRepoKey());
                    return repo != null && isResultAcceptable(item.getRepoPath(), repo);
                })
                .map(item -> new StatsSearchResult(item, getRepoService().getStatsInfo(item.getRepoPath())))
                .collect(Collectors.toList());
    }
}

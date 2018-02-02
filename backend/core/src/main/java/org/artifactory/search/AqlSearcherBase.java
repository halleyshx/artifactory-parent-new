/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.search;

import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.search.SearchControls;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.storage.spring.StorageContextHelper;

import java.util.List;

import static org.artifactory.aql.api.internal.AqlBase.or;

/**
 * Currently oriented towards AqlApiItem, generify if you need
 *
 * @author Dan Feldman
 */
public abstract class AqlSearcherBase<C extends SearchControls, R extends ItemSearchResult> extends SearcherBase<C, R> {

    /**
     * @return a clause which will filter results based on {@link SearchControls#getSelectedRepoForSearch()}
     * The artifact should exist in at least one of the repositories therefore the relation between the repos is OR
     */
    protected AqlBase.OrClause<AqlApiItem> getSelectedReposForSearchClause(SearchControls controls) {
        List<String> selectedRepoForSearch = controls.getSelectedRepoForSearch();
        AqlBase.OrClause<AqlApiItem> reposToSearchClause = or();
        if (selectedRepoForSearch != null) {
            for (String repoKey : selectedRepoForSearch) {
                reposToSearchClause.append(
                        AqlApiItem.repo().equal(repoKey)
                );
            }
        }
        return reposToSearchClause;
    }

    protected AqlEagerResult<AqlItem> executeQuery(AqlBase.AndClause<AqlApiItem> query, SearchControls controls) {
        AqlApiItem aqlQuery = AqlApiItem.create().filter(query);
        return executeQuery(aqlQuery, controls);
    }

    protected AqlEagerResult<AqlItem> executeQuery(AqlApiItem aqlQuery, SearchControls controls) {
        aqlQuery.limit((getLimit(controls)));
        AqlService aqlService = StorageContextHelper.get().beanForType(AqlService.class);
        return aqlService.executeQueryEager(aqlQuery);
    }

}

package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * A package search result merger.
 * <p>
 * A merger is called after all results collected and enables to merge a collection of results into a single result.
 * </p>
 * @author Yinon Avraham
 * Created on 08/09/2016.
 * @see DummyPackageSearchResultMerger
 */
public interface PackageSearchResultMerger {

    /**
     * Get a key by which the given result should be merged
     * @param result the result for which to get the merge key
     * @return the merge key
     */
    @Nonnull
    String getMergeKey(PackageSearchResult result);

    /**
     * Merge the given results into a single result
     * @param packageSearchResults the set of results to be merged
     * @return the merged result
     */
    @Nonnull
    PackageSearchResult merge(Set<PackageSearchResult> packageSearchResults);

    /**
     * Indicator for whether the merger should be used always, even if there is a single entry.
     */
    boolean isOperateOnSingleEntry();

}

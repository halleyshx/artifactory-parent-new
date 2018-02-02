package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * A dummy package search result merger that does nothing - merge by repo path (which is expected to be unique).
 * @author Yinon Avraham
 * Created on 08/09/2016.
 */
public class DummyPackageSearchResultMerger implements PackageSearchResultMerger {

    public static final PackageSearchResultMerger DUMMY_MERGER = new DummyPackageSearchResultMerger();

    private DummyPackageSearchResultMerger() {}

    @Override
    @Nonnull
    public String getMergeKey(PackageSearchResult result) {
        return result.getRepoPath().toPath();
    }

    @Override
    @Nonnull
    public PackageSearchResult merge(Set<PackageSearchResult> packageSearchResults) {
        assert packageSearchResults.size() == 1;
        return packageSearchResults.iterator().next();
    }

    @Override
    public boolean isOperateOnSingleEntry() {
        return false;
    }
}

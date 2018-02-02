package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * A result merger for Conan package search.
 * We search and find both recipe files and package binary files, but want to display only the recipe and want to
 * display it once. The result can contain a recipe file, package files or both.
 * @author Yinon Avraham
 * Created on 08/09/2016.
 */
public class ConanPackageSearchResultMerger implements PackageSearchResultMerger {

    private static final Logger log = LoggerFactory.getLogger(ConanPackageSearchResultMerger.class);

    @Override
    @Nonnull
    public String getMergeKey(PackageSearchResult result) {
        Map<String, Collection<String>> extraFields = result.getExtraFields();
        String name = getFirstFieldValue(extraFields, PackageSearchCriteria.conanName);
        String version = getFirstFieldValue(extraFields, PackageSearchCriteria.conanVersion);
        String user = getFirstFieldValue(extraFields, PackageSearchCriteria.conanUser);
        String channel = getFirstFieldValue(extraFields, PackageSearchCriteria.conanChannel);
        //Merge by Conan recipe folder path
        return String.join("/", name, version, user, channel);
    }

    private String getFirstFieldValue(Map<String, Collection<String>> extraFields, PackageSearchCriteria criteria) {
        return extraFields.get(criteria.getModel().getId()).iterator().next();
    }

    @Override
    @Nonnull
    public PackageSearchResult merge(Set<PackageSearchResult> packageSearchResults) {
        // Find the last modified result
        PackageSearchResult sample = packageSearchResults.stream()
                //Sort by modification date to get the most recently modified result
                .sorted((r1, r2) -> -Long.compare(r1.getModifiedDate(), r2.getModifiedDate()))
                .findFirst().get();
        log.debug("Selected sample package search result (out of {} results): {}", packageSearchResults.size(), sample.getRepoPath());
        // name/version/user/channel/...
        String[] parts = sample.getRepoPath().getPath().split("/");
        String recipeFolderPath = String.join("/", parts[0], parts[1], parts[2], parts[3]);
        RepoPath recipeFolderRepoPath = InfoFactoryHolder.get().createRepoPath(sample.getRepoKey(), recipeFolderPath);
        log.debug("Setting recipe folder repo path: {}", recipeFolderRepoPath);
        //Assign the last modified from the sample (which is the latest)
        Date modifiedDate = new Date(sample.getModifiedDate());
        PackageSearchResult result = new PackageSearchResult(recipeFolderRepoPath, modifiedDate, sample.getPackageType());
        result.setModifiedString(sample.getModifiedString());
        result.getExtraFieldsMap().putAll(sample.getExtraFieldsMap());
        result.setName(createConanName(result));
        return result;
    }

    private String createConanName(PackageSearchResult result) {
        Map<String, Collection<String>> fieldsMap = result.getExtraFields();
        String conanName = getFirstFieldValue(fieldsMap, PackageSearchCriteria.conanName);
        String conanVersion = getFirstFieldValue(fieldsMap, PackageSearchCriteria.conanVersion);
        String conanUser = getFirstFieldValue(fieldsMap, PackageSearchCriteria.conanUser);
        String conanChannel = getFirstFieldValue(fieldsMap, PackageSearchCriteria.conanChannel);
        if (conanName != null && conanVersion != null && conanUser != null && conanChannel != null) {
            return conanName + "/" + conanVersion + "@" + conanUser + "/" + conanChannel;
        }
        return result.getName();
    }

    @Override
    public boolean isOperateOnSingleEntry() {
        return true;
    }
}

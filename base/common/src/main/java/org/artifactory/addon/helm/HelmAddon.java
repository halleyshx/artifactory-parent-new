package org.artifactory.addon.helm;

import org.artifactory.addon.Addon;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.fs.VfsItem;

/**
 * @author Nadav Yogev
 * @author Yuval Reches
 */
public interface HelmAddon extends Addon {

    /**
     * Adds a Helm chart to the registry, index it and add it to the index.yaml
     * Adding the Chart to a queue of events and invoking local index calculation
     *
     * @param fsItem Chart file added
     */
    void addHelmPackage(VfsItem fsItem);

    /**
     * Removes a Helm Chart from the repository and the index.yaml
     * Adding the Chart's name and version to a queue of events and invoking local index calculation
     * The removal is based on a Chart's name and version.
     * Those values are either extracted from the file's properties, or from the metadata itself if still present.
     * In case both are missing --> Chart won't be removed from index file.
     */
    void removeHelmPackage(VfsItem fsItem);

    /**
     * Request Helm metadata calculation based on a repo key.
     * We use the Helm Service and going threw the Async annotations based on the async param
     */
    void requestHelmMetadataCalculation(RepoPath path, boolean async);

    /**
     * Request Helm metadata virtual calculation based on a request url.
     * We generate a index file with URLs based on the requestUrl.
     */
    RepoPath requestVirtualHelmCustomMetadataCalculation(RepoPath path, String requestUrl);

    /**
     * Request Helm metadata calculation of the entire repo
     * We use the Helm Service and going threw the Async annotations based on the async param
     */
    void requestReindexRepo(String repoKey, boolean async);

    /**
     * Used to evict all virtual repositories custom index.yaml files for all virtual repositories containing the
     * given repository, to force recalculation of all custom request url based requests.
     */
    void invokeVirtualMetadataEviction(RealRepoDescriptor descriptor);

    /**
     * Used to extract helm metadata for a given helm chart
     */
    HelmMetadataInfo getMetadataToUiModel(RepoPath repoPath);
}

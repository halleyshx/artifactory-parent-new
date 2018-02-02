package org.artifactory.addon.chef;

import org.artifactory.addon.Addon;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;

/**
 * @author Alexis Tual
 */
public interface ChefAddon extends Addon {

    void addCookbook(FileInfo info);

    /**
     * @param repoPath path to an Artifact
     * @return the Chef Cookbook informations corresponding to the given Artifact.
     */
    ChefCookbookInfo getChefCookbookInfo(RepoPath repoPath);

    /**
     * @param fileName file name
     * @return true if the filename corresponds to a Chef Cookbook
     */
    boolean isChefCookbookFile(String fileName);

    /**
     * Should recalculate the Cookbook for all index in repo
     *
     * @param repoKey    a repo key
     * @param indexAsync true if the indexing will be done asynchronously, false to wait for the outcome of the index.
     */
    void recalculateAll(String repoKey, boolean indexAsync);

    /**
     * Calculate the root index (api/v1/cookbooks) for a virtual repository from all the aggregated repositories.
     */
    void calculateVirtualRepoMetadata(String repoKey, String baseUrl);
}

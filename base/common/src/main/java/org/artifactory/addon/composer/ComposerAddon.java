package org.artifactory.addon.composer;

import org.artifactory.addon.Addon;
import org.artifactory.repo.RepoPath;

/**
 * @author Shay Bagants
 */
public interface ComposerAddon extends Addon {

    /**
     * Handle new package deployment by marking the artifact with the composer properties and trigger async indexing
     * of the package
     *
     * @param repoPath The file repoPath
     */
    void handlePackageDeployment(RepoPath repoPath);

    /**
     * Handle package deletion
     *
     * @param repoPath The repoPath of the deleted package
     */
    void handlePackageDeletion(RepoPath repoPath);

    /**
     * Re index the entire repository content
     *
     * @param repoKey The repo key
     * @param async   Calculate async or not
     */
    void recalculateAll(String repoKey, boolean async);

    /**
     * Check whether the file name extension matches the supported extensions
     */
    boolean isComposerSupportedExtension(String fileName);

    /**
     * Return a model of the metadata info. Should be used to retrieve a package information into the UI tab
     *
     * @param repoPath The path of the artifact to retrieve the metadata
     * @return An ComposerMetadataInfo object with the package metadata information
     */
    ComposerMetadataInfo getComposerMetadataInfo(RepoPath repoPath);
}

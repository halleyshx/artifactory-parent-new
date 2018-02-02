package org.artifactory.addon.conan;

import org.artifactory.addon.Addon;
import org.artifactory.addon.conan.info.ConanPackageInfo;
import org.artifactory.addon.conan.info.ConanRecipeInfo;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;

/**
 * @author Yinon Avraham
 * Created on 03/09/2016.
 */
public interface ConanAddon extends Addon {

    /**
     * Check whether the given repo path is a conan reference folder
     * @param repoPath the repo path to check
     * @return {@code true} if the repo path is a conan reference folder, {@code false} otherwise
     */
    boolean isConanReferenceFolder(@Nonnull RepoPath repoPath);

    /**
     * Check whether the given repo path is a conan binary package folder
     * @param repoPath the repo path to check
     * @return {@code true} if the repo path is a conan binary package folder, {@code false} otherwise
     */
    boolean isConanPackageFolder(@Nonnull RepoPath repoPath);

    /**
     * Get the information of the conan recipe under the given repo path
     * @param repoPath the repo path under which the recipe is expected
     * @return the conan recipe information
     * @throws IllegalArgumentException if a recipe does not exist under the given repo path
     * @see #isConanReferenceFolder(RepoPath)
     * @see #isConanPackageFolder(RepoPath)
     */
    @Nonnull
    ConanRecipeInfo getRecipeInfo(@Nonnull RepoPath repoPath);

    /**
     * Get the information of the conan binary package under the given repo path
     * @param repoPath the repo path under which the binary package is expected
     * @return the conan binary package information
     * @throws IllegalArgumentException if a binary package does not exist under the given repo path
     * @see #isConanPackageFolder(RepoPath)
     */
    @Nonnull
    ConanPackageInfo getPackageInfo(@Nonnull RepoPath repoPath);

    /**
     * Count the conan binary packages under the given path
     * @param repoPath the repo path under which to count the packages
     * @return the number of packages
     */
    int countPackages(@Nonnull RepoPath repoPath);
}

package org.artifactory.addon.xray;

import org.artifactory.addon.Addon;
import org.artifactory.api.repo.Async;
import org.artifactory.exception.UnsupportedOperationException;
import org.artifactory.repo.RepoPath;
import org.jfrog.build.api.Build;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Chen Keinan
 */
public interface XrayAddon extends Addon {

    //The user that's set up in Artifactory to allow Xray access to various resources
    String ARTIFACTORY_XRAY_USER = "xray";

    //None is not a real xray status. it is used for no status.
    String INDEX_STATUS_NONE = "None";
    String INDEX_STATUS_PENDING = "Pending";
    String INDEX_STATUS_INDEXING = "Indexing";
    String INDEX_STATUS_INDEXED = "Indexed";
    String INDEX_STATUS_SCANNED = "Scanned";

    String ALERT_SEVERITY_NONE = "None";
    String ALERT_SEVERITY_MINOR = "Minor";
    String ALERT_SEVERITY_MAJOR = "Major";
    String ALERT_SEVERITY_CRITICAL = "Critical";

    boolean isXrayConfigExist();

    boolean isXrayEnabled();

    InputStream scanBuild(XrayScanBuild xrayScanBuild) throws IOException;

    String createXrayUser();

    void removeXrayConfig();

    void deleteXrayUser(String xrayUser);

    void setXrayEnabledOnRepos(List<XrayRepo> repos, boolean enabled);

    /**
     * Get the list of repositories that should be enabled with 'xrayIndex'
     *
     * @param repos The list of repositories that should be enabled with 'xrayIndex'
     */
    void updateXraySelectedIndexedRepos(List<XrayRepo> repos);

    /**
     * Get the number of artifact that are marked with "Indexed"
     *
     * @param repoKey The repository to search on
     * @return The count result
     */
    int getXrayIndexedCountForRepo(String repoKey);

    /**
     * Get the number of artifact that are marked with "Indexing"
     *
     * @param repoKey The repository to search on
     * @return The count result
     */
    int getXrayIndexingCountForRepo(String repoKey);

    /**
     * Get the number of artifact that are potential for xray index
     *
     * @param repoKey The repository to search on
     * @return The count result
     */
    int getXrayPotentialCountForRepo(String repoKey) throws UnsupportedOperationException;

    List<XrayRepo> getXrayIndexedAndNonIndexed(boolean indexed);

    /**
     * Sends Xray event for a new build
     * @param build holds the build name and number
     */
    void callAddBuildInterceptor(Build build);

    /**
     * Sends Xray event for build deletion
     * @param buildName Build Name
     * @param buildNumber Build Number
     */
    void callDeleteBuildInterceptor(String buildName, String buildNumber);

    @Async
    void indexRepos(List<String> repos);

    void clearAllIndexTasks();

    void blockXrayGlobally() throws OperationNotSupportedException;

    void unblockXrayGlobally() throws OperationNotSupportedException;

    @Nonnull
    ArtifactXrayInfo getArtifactXrayInfo(@Nonnull RepoPath path);

    /**
     * @return true if {@param path} should be blocked for download according to its storing repo's xray configuration.
     */
    boolean isDownloadBlocked(RepoPath path);

    /**
     * true if xray can index this file
     */
    boolean isHandledByXray(RepoPath path);

    /**
     * Sets xray.{uid}.alert.ignored = {@param ignored} on {@param path}
     */
    void setAlertIgnored(boolean ignored, RepoPath path);

    /**
     * Kinda does what {@link org.artifactory.api.repo.RepositoryService#getChildrenDeeply} does but checks for
     * xray-blocked artifacts on the fly, once the predefined limit (set in the impl class) is reached the tree
     * traversal breaks off.
     */
    List<RepoPath> getBlockedPathsUnder(RepoPath folder);
}

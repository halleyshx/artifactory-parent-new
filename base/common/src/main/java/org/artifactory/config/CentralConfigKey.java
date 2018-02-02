package org.artifactory.config;

import org.jfrog.common.DiffUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Noam Shemesh
 */
public enum CentralConfigKey {
    none("-"), // Listen to none to prevent reloads
    proxies("proxies"),
    security("security"),
    securityPasswordSettings("security.passwordSettings"),
    securityAccessClientSettings("security.accessClientSettings"),
    securityCrowdSettings("security.crowdSettings"),
    remoteReplications("remoteReplications"),
    localReplications("localReplications"),
    localRepositoriesMap("localRepositoriesMap"),
    virtualRepositoriesMap("virtualRepositoriesMap"),
    remoteRepositoriesMap("remoteRepositoriesMap"),
    singleReplicationPerRepoMap("singleReplicationPerRepoMap"),
    distributionRepositoriesMap("distributionRepositoriesMap"),
    folderDownloadConfig("folderDownloadConfig"),
    backups("backups"),
    indexer("indexer"),
    sumoLogicConfig("sumoLogicConfig"),
    urlBase("urlBase"),
    serverName("serverName"),
    cleanupConfig("cleanupConfig"),
    virtualCacheCleanupConfig("virtualCacheCleanupConfig"),
    gcConfig("gcConfig"),
    offlineMode("offlineMode"),
    xrayConfig("xrayConfig");

    private final String key;

    CentralConfigKey(@Nonnull String key) {
        this.key = Objects.requireNonNull(key, "Key must not be null").replaceAll("[.]", DiffUtils.DELIMITER);
    }

    public String getKey() {
        return key;
    }
}

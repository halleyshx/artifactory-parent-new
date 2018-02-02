package org.artifactory.repo.service.versioning;

import org.artifactory.repo.*;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.storage.db.security.service.BasicCacheModel;

import java.util.Map;
import java.util.Set;

/**
 * @author gidis
 */
public class RepositoriesCache implements BasicCacheModel {

    private long version;
    public final LocalRepo trashcan;
    public final Set<String> allRepoKeysCache;
    public final Map<String, LocalRepo> localRepositoriesMap;
    public final Map<String, RemoteRepo> remoteRepositoriesMap;
    public final Map<String, LocalCacheRepo> localCacheRepositoriesMap;
    public final Map<String, VirtualRepo> virtualRepositoriesMap;
    public final Map<String, DistributionRepo> distributionRepositoriesMap;

    public RepositoriesCache(Map<String, DistributionRepo> distributionRepositoriesMap,
            Map<String, VirtualRepo> virtualRepositoriesMap,
            Map<String, LocalCacheRepo> localCacheRepositoriesMap,
            Map<String, RemoteRepo> remoteRepositoriesMap,
            Map<String, LocalRepo> localRepositoriesMap, LocalRepo trashcan,
            Set<String> allRepoKeysCache) {
        this.distributionRepositoriesMap = distributionRepositoriesMap;
        this.virtualRepositoriesMap = virtualRepositoriesMap;
        this.localCacheRepositoriesMap = localCacheRepositoriesMap;
        this.remoteRepositoriesMap = remoteRepositoriesMap;
        this.localRepositoriesMap = localRepositoriesMap;
        this.trashcan = trashcan;
        this.allRepoKeysCache = allRepoKeysCache;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public void destroy() {
        localRepositoriesMap.values().forEach(Repo::close);
        remoteRepositoriesMap.values().forEach(Repo::close);
        localCacheRepositoriesMap.values().forEach(Repo::close);
        virtualRepositoriesMap.values().forEach(Repo::close);
        distributionRepositoriesMap.values().forEach(Repo::close);
    }
}

package org.artifactory.storage.db.security.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.storage.security.service.AclStoreService;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author nadavy
 */
public class AclCacheLoader implements Callable<AclCacheLoader.AclCacheItem> {

    private AclStoreService aclStoreService;

    private static class PrincipalToPermissions extends HashMap<String, Map<String, Set<AclInfo>>> {
    }

    public AclCacheLoader(AclStoreService aclStoreService) {
        this.aclStoreService = aclStoreService;
    }

    /**
     * gets and updates AclCache from DB, when call by db promotion
     * This call creates user and group ACL cache mappers
     * key: user/group name
     * value: map of user/group repo keys of given user/group to respected repo key ACL (Access Control List)
     * ACL will still have all of their ACE (Access Control Entries)
     *
     * @return up-to-date AclCacheItem
     */
    @Override
    public AclCacheItem call() {
        Collection<AclInfo> allAcls = aclStoreService.getDownstreamAllAcls();

        Map<String, AclInfo> aclResultMap = Maps.newHashMapWithExpectedSize(allAcls.size());
        // user and group cache mappers. mapper key is username/group name
        // mapper value is a mapper of repo keys to a set of AclInfos
        PrincipalToPermissions userResultMap = new PrincipalToPermissions();
        PrincipalToPermissions groupResultMap = new PrincipalToPermissions();

        for (AclInfo acl : allAcls) {
            PermissionTargetInfo permissionTarget = acl.getPermissionTarget();
            Set<AceInfo> dbAces = acl.getAces();

            for (AceInfo ace : dbAces) {
                addToMap(ace.isGroup() ? groupResultMap : userResultMap, acl, ace.getPrincipal());
            }

            aclResultMap.put(permissionTarget.getName(), acl);
        }
        return new AclCacheItem(aclResultMap, userResultMap, groupResultMap);
    }

    private void addToMap(PrincipalToPermissions resultMap,
            AclInfo acl,
            String name) {
        if (name != null) {
            // populate group result map with given ace
            addPermissionTargetToResultMap(resultMap, name, acl);
        }
    }

    /**
     * Creates or add a user/group map to a aclInfo in AclCache user or group cache.
     *
     * @param resultMap group or user result map to add repokey/aclInfo to
     * @param key       username or group name for key
     * @param aclInfo   aclInfo to add for value
     */
    private void addPermissionTargetToResultMap(PrincipalToPermissions resultMap, String key,
            AclInfo aclInfo) {
        Map<String, Set<AclInfo>> repoKeyMap = resultMap.computeIfAbsent(key, map -> Maps.newHashMap());
        List<String> repoKeys = aclInfo.getPermissionTarget().getRepoKeys();
        repoKeys.forEach(repoKey -> addRepoKeyToMap(repoKeyMap, repoKey, aclInfo));
    }

    /**
     * Add repo key to a user/group cache mapper, with an AclInfo
     *  @param map     specific user/group map of repo keys to aclInfos set
     * @param repoKey repokey to add
     * @param aclInfo aclInfo to add
     */
    private void addRepoKeyToMap(Map<String, Set<AclInfo>> map, String repoKey, AclInfo aclInfo) {
        Set<AclInfo> aclInfos = map.computeIfAbsent(repoKey, info -> Sets.newHashSet());
        aclInfos.add(InfoFactoryHolder.get().createAcl(aclInfo.getPermissionTarget(), aclInfo.getAces(), aclInfo.getUpdatedBy()));
    }

    public static class AclCacheItem implements BasicCacheModel{
        // acl name to acl info.
        private final Map<String, AclInfo> aclInfoMap;
        // Maps of user/group name to -> map of repo path to aclInfo
        private final PrincipalToPermissions userResultMap;
        private final PrincipalToPermissions groupResultMap;
        private long version;

        public AclCacheItem(Map<String, AclInfo> aclInfoMap, PrincipalToPermissions userResultMap,
                PrincipalToPermissions groupResultMap) {
            this.aclInfoMap = aclInfoMap;
            this.userResultMap = userResultMap;
            this.groupResultMap = groupResultMap;
        }

        @Override
        public long getVersion() {
            return version;
        }

        @Override
        public void setVersion(long version) {
            this.version = version;
        }

        public Map<String, AclInfo> getAclInfoMap() {
            return aclInfoMap;
        }

        public Map<String, Map<String, Set<AclInfo>>> getUserResultMap() {
            return userResultMap;
        }

        public Map<String, Map<String, Set<AclInfo>>> getGroupResultMap() {
            return groupResultMap;
        }


        @Override
        public void destroy() {

        }
    }
}
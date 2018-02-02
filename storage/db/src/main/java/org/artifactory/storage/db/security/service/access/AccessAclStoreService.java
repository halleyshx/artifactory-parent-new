/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.storage.db.security.service.access;

import org.artifactory.common.ConstantValues;
import org.artifactory.event.CacheType;
import org.artifactory.event.InvalidateCacheEvent;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.AclInfo;
import org.artifactory.security.MutableAclInfo;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.security.access.AccessService;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.security.service.AclCacheLoader;
import org.artifactory.storage.db.security.service.VersioningCache;
import org.artifactory.storage.security.service.AclCache;
import org.artifactory.storage.security.service.AclStoreService;
import org.jfrog.access.client.AccessClientException;
import org.jfrog.access.client.permission.PermissionsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.artifactory.storage.db.security.service.access.AclMapper.*;

/**
 * @author Noam Shemesh
 */
@Service
@Primary
public class AccessAclStoreService implements AclStoreService, ApplicationListener<InvalidateCacheEvent> {

    private VersioningCache<AclCacheLoader.AclCacheItem> aclsCache;
    private AccessService accessService;

    @Autowired
    public void setAccessService(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostConstruct
    private void init() {
        long timeout = ConstantValues.aclDirtyReadsTimeout.getLong();
        aclsCache = new VersioningCache<>(timeout, new AclCacheLoader(this));
    }

    @Override
    public Collection<AclInfo> getAllAcls() {
        return getAclsMap().values();
    }

    @Override
    public Collection<AclInfo> getDownstreamAllAcls() {
        return accessService.getAccessClient().permissions()
                .findPermissionsByServiceId(accessService.getArtifactoryServiceId()).getPermissions()
                .stream()
                .map(permission -> toArtifactoryAcl(permission, getServiceId()))
                .collect(Collectors.toSet());
    }

    @Override
    public AclCache getAclCache() {
        AclCacheLoader.AclCacheItem aclCacheItem = aclsCache.get();
        return new AclCache(aclCacheItem.getGroupResultMap(), aclCacheItem.getUserResultMap());
    }

    @Override
    public AclInfo getAcl(String permTargetName) {
        return getAclsMap().get(permTargetName);
    }

    @Override
    public boolean permissionTargetExists(String permTargetName) {
        return getAclsMap().containsKey(permTargetName);
    }

    @Override
    public void createAcl(AclInfo entity) {
        cudOperation(permClient -> permClient.createPermission(toAccessPermission(entity, getServiceId())),
                "Could not create " + entity);
    }

    @Override
    public void updateAcl(MutableAclInfo entity) {
        cudOperation(permClient -> permClient.replacePermission(toAccessPermission(entity, getServiceId())),
                "Could not update " + entity);
    }

    @Override
    public void deleteAcl(String permTargetName) {
        cudOperation(permClient -> permClient.deletePermissionByName(toAccessName(getServiceId(), permTargetName)),
                "Could not delete " + permTargetName);
    }

    private void cudOperation(Consumer<PermissionsClient> execute, String possibleErrorMessage) {
        try {
            execute.accept(accessService.getAccessClient().permissions());
        } catch (IllegalArgumentException | AccessClientException e) {
            throw new StorageException(possibleErrorMessage, e);
        } finally {
            invalidateAclCache();
        }
    }

    @Override
    public void removeAllUserAces(String username) {
        removeAllGeneric(username, getAclCache().getUserResultMap().get(username), false);
    }

    @Override
    public void removeAllGroupAces(String groupName) {
        removeAllGeneric(groupName, getAclCache().getGroupResultMap().get(groupName), true);
    }

    private void removeAllGeneric(String principal, Map<String, Set<AclInfo>> repoToAcls, boolean isGroup) {
        if (repoToAcls == null) {
            return;
        }
        cudOperation((permissionsClient) -> repoToAcls
                .values()
                .stream()
                .flatMap(Set::stream)
                .map(acl -> removeAceFromAcl(acl, principal, isGroup))
                .forEach(this::updateAcl), "Could not delete ACE for " + (isGroup ? "group" : "user") + principal);
    }

    private MutableAclInfo removeAceFromAcl(AclInfo aclInfo, String username, boolean isGroup) {
        MutableAclInfo mutableAclInfo = InfoFactoryHolder.get().copyAcl(aclInfo);
        mutableAclInfo.setAces(aclInfo.getAces()
                .stream()
                .filter(ace -> ace.isGroup() != isGroup || (ace.isGroup() == isGroup && !ace.getPrincipal().equals(username)))
                .collect(Collectors.toSet()));
        return mutableAclInfo;
    }

    @Override
    public void deleteAllAcls() {
        cudOperation((permissionsClient) -> getAllAcls()
                .stream()
                .map(AclInfo::getPermissionTarget)
                .map(PermissionTargetInfo::getName)
                .forEach(this::deleteAcl), "Could not delete all ACLs");
    }

    @Override
    public int invalidateAclCache() {
        return aclsCache.promoteDbVersion();
    }

    private String getServiceId() {
        return accessService.getArtifactoryServiceId().getFormattedName();
    }

    private Map<String, AclInfo> getAclsMap() {
        return aclsCache.get().getAclInfoMap();
    }

    @Override
    public void onApplicationEvent(InvalidateCacheEvent invalidateCacheEvent) {
        if (CacheType.ACL.equals(invalidateCacheEvent.getCacheType())) {
            invalidateAclCache();
        }
    }
}

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

package org.artifactory.storage.db.security.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.artifactory.common.ConstantValues;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.*;
import org.artifactory.storage.DBEntityNotFoundException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.security.dao.AclsDao;
import org.artifactory.storage.db.security.dao.PermissionTargetsDao;
import org.artifactory.storage.db.security.dao.UserGroupsDao;
import org.artifactory.storage.db.security.entity.Ace;
import org.artifactory.storage.db.security.entity.Acl;
import org.artifactory.storage.db.security.entity.PermissionTarget;
import org.artifactory.storage.db.security.service.access.StringLongCodec;
import org.artifactory.storage.security.service.AclCache;
import org.artifactory.storage.security.service.AclStoreService;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.util.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 9/3/12
 * Time: 4:12 PM
 *
 * @author freds
 */
@Service
@Deprecated
public class AclStoreServiceImpl implements AclStoreService {
    private static final Logger log = LoggerFactory.getLogger(AclStoreServiceImpl.class);
    private DbService dbService;
    private AclsDao aclsDao;
    private PermissionTargetsDao permTargetsDao;
    private UserGroupsDao userGroupsDao;
    private UserGroupStoreService accessUserStore;

    @Autowired
    public AclStoreServiceImpl(DbService dbService, AclsDao aclsDao,
            PermissionTargetsDao permTargetsDao, UserGroupsDao userGroupsDao,
            UserGroupStoreService accessUserStore) {
        this.dbService = dbService;
        this.aclsDao = aclsDao;
        this.permTargetsDao = permTargetsDao;
        this.userGroupsDao = userGroupsDao;
        this.accessUserStore = accessUserStore;
    }

    private VersioningCache<AclCacheLoader.AclCacheItem> aclsCache;

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
        try {
            Map<Long, PermissionTargetInfo> targetMap = permTargetsDao.getAllPermissionTargets().entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey(), createPermissionTarget(entry.getValue())))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            return getDownstreamAllAcls(targetMap);
        } catch (SQLException e) {
            throw new StorageException("Could not get all permissions targets ", e);
        }
    }

    private Collection<AclInfo> getDownstreamAllAcls(Map<Long, PermissionTargetInfo> targetMap) {
        try {
            return aclsDao.getAllAcls().stream()
                    .map(acl -> infoFromAcl(groupsNamesByIds(), usersNamesByIds(), targetMap, acl))
                    .collect(Collectors.toSet());
        } catch (SQLException e) {
            throw new StorageException("Could not get all acls ", e);
        }
    }

    private Map<Long,String> groupsNamesByIds() {
        try {
            return userGroupsDao.getAllGroupNamePerIds();
        } catch (SQLException e) {
            throw new StorageException("Could not get all acls ", e);
        }
    }

    private Map<Long,String> usersNamesByIds() {
        try {
            return userGroupsDao.getAllUsernamePerIds();
        } catch (SQLException e) {
            throw new StorageException("Could not get all acls ", e);
        }
    }

    private AclInfo infoFromAcl(Map<Long, String> groups, Map<Long, String> users,
            Map<Long, PermissionTargetInfo> targetMap, Acl acl) {
        return InfoFactoryHolder.get().createAcl(targetMap.get(acl.getPermTargetId()),
                convertAces(groups, users, acl.getAces()), acl.getLastModifiedBy());
    }

    private Set<AceInfo> convertAces(Map<Long, String> groups, Map<Long, String> users, ImmutableSet<Ace> aces) {
        return aces.stream().map(
                ace -> InfoFactoryHolder.get().createAce(
                        ace.isOnGroup() ? groups.get(ace.getGroupId()) : users.get(ace.getUserId()),
                        ace.isOnGroup(),
                        ace.getMask())
        ).collect(Collectors.toSet());
    }

    private PermissionTargetInfo createPermissionTarget(PermissionTarget permTarget) {
        MutablePermissionTargetInfo info = InfoFactoryHolder.get().createPermissionTarget(
                permTarget.getName(), new ArrayList<>(permTarget.getRepoKeys()));
        info.setIncludes(permTarget.getIncludes());
        info.setExcludes(permTarget.getExcludes());

        return info;
    }

    @Override
    public AclCache getAclCache() {
        AclCacheLoader.AclCacheItem aclCacheItem = aclsCache.get();
        return new AclCache(aclCacheItem.getGroupResultMap(), aclCacheItem.getUserResultMap());
    }

    @Override
    public void createAcl(AclInfo entity) {
        try {
            PermissionTargetInfo permTargetInfo = entity.getPermissionTarget();
            PermissionTarget dbPermTarget = permTargetsDao.findPermissionTarget(permTargetInfo.getName());
            if (dbPermTarget != null) {
                throw new AlreadyExistsException("Could not create ACL. Permission target already exist: " +
                        permTargetInfo.getName());
            }

            dbPermTarget = new PermissionTarget(dbService.nextId(), permTargetInfo.getName(),
                    permTargetInfo.getIncludes(), permTargetInfo.getExcludes());
            dbPermTarget.setRepoKeys(Sets.newHashSet(permTargetInfo.getRepoKeys()));
            permTargetsDao.createPermissionTarget(dbPermTarget);
            Acl acl = aclFromInfo(dbService.nextId(), entity, dbPermTarget.getPermTargetId());
            aclsDao.createAcl(acl);
        } catch (SQLException e) {
            throw new StorageException("Could not create ACL " + entity, e);
        } finally {
            aclsCache.promoteDbVersion();
        }
    }

    @Override
    public void updateAcl(MutableAclInfo aclInfo) {
        PermissionTargetInfo permTargetInfo = aclInfo.getPermissionTarget();
        try {
            PermissionTarget dbPermTarget = permTargetsDao.findPermissionTarget(permTargetInfo.getName());
            if (dbPermTarget == null) {
                throw new DBEntityNotFoundException(
                        "Could not update ACL with non existent Permission Target " + aclInfo.getPermissionTarget());
            }
            long permTargetId = dbPermTarget.getPermTargetId();
            PermissionTarget newPermTarget = new PermissionTarget(permTargetId,
                    permTargetInfo.getName(), permTargetInfo.getIncludes(), permTargetInfo.getExcludes());
            newPermTarget.setRepoKeys(Sets.newHashSet(permTargetInfo.getRepoKeys()));
            permTargetsDao.updatePermissionTarget(newPermTarget);
            Acl dbAcl = aclsDao.findAclByPermissionTargetId(permTargetId);
            if (dbAcl == null) {
                throw new DBEntityNotFoundException("Could not update non existent ACL " + aclInfo);
            }
            Acl acl = aclFromInfo(dbAcl.getAclId(), aclInfo, permTargetId);
            aclsDao.updateAcl(acl);
        } catch (SQLException e) {
            throw new StorageException("Could not update ACL " + aclInfo, e);
        } finally {
            aclsCache.promoteDbVersion();
        }
    }

    @Override
    public void deleteAcl(String permTargetName) {
        try {
            PermissionTarget permissionTarget = permTargetsDao.findPermissionTarget(permTargetName);
            if (permissionTarget == null) {
                // Already deleted
                return;
            }
            Acl acl = aclsDao.findAclByPermissionTargetId(permissionTarget.getPermTargetId());
            if (acl != null) {
                aclsDao.deleteAcl(acl.getAclId());
            } else {
                log.warn("ACL already deleted, but permission target was not!");
            }
            permTargetsDao.deletePermissionTarget(permissionTarget.getPermTargetId());
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACL " + permTargetName, e);
        } finally {
            aclsCache.promoteDbVersion();
        }
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
    public void removeAllUserAces(String username) {
        try {
            long userId = StringLongCodec.encode(username);
            aclsDao.deleteAceForUser(userId);
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACE for user " + username, e);
        } finally {
            aclsCache.promoteDbVersion();
        }
    }

    @Override
    public void removeAllGroupAces(String groupName) {
        try {
            long groupId = StringLongCodec.encode(groupName);
            aclsDao.deleteAceForGroup(groupId);
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACE for group " + groupName, e);
        } finally {
            aclsCache.promoteDbVersion();
        }
    }

    @Override
    public void deleteAllAcls() {
        try {
            aclsDao.deleteAllAcls();
            permTargetsDao.deleteAllPermissionTargets();
        } catch (SQLException e) {
            throw new StorageException("Could not delete all ACLs", e);
        } finally {
            aclsCache.promoteDbVersion();
        }
    }

    @Override
    public int invalidateAclCache() {
        return aclsCache.promoteDbVersion();
    }

    private Acl aclFromInfo(long aclId, AclInfo aclInfo, long permTargetId) throws SQLException {
        Acl acl = new Acl(aclId, permTargetId, System.currentTimeMillis(),
                aclInfo.getUpdatedBy());
        Set<AceInfo> aces = aclInfo.getAces();
        HashSet<Ace> dbAces = new HashSet<>(aces.size());
        for (AceInfo ace : aces) {
            Ace dbAce = null;
            if (ace.isGroup()) {
                GroupInfo group = accessUserStore.findGroup(ace.getPrincipal());
                if (group != null) {
                    dbAce = new Ace(dbService.nextId(), acl.getAclId(), ace.getMask(), 0,
                            StringLongCodec.encode(group.getGroupName()));
                } else {
                    log.error("Got ACE entry for ACL " + aclInfo.getPermissionTarget().getName() +
                            " with a group " + ace.getPrincipal() + " that does not exist!");
                }
            } else {
                UserInfo user = accessUserStore.findUser(ace.getPrincipal());
                if (user != null) {
                    dbAce = new Ace(dbService.nextId(), acl.getAclId(), ace.getMask(),
                            StringLongCodec.encode(user.getUsername()), 0);
                } else {
                    log.error("Got ACE entry for ACL " + aclInfo.getPermissionTarget().getName() +
                            " with a user " + ace.getPrincipal() + " that does not exist!");
                }
            }
            if (dbAce != null) {
                dbAces.add(dbAce);
            }
        }
        acl.setAces(dbAces);
        return acl;
    }

    private Map<String, AclInfo> getAclsMap() {
        return aclsCache.get().getAclInfoMap();
    }

}

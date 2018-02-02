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

package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.permission;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.EffectivePermission;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.PathEffectivePermissions;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.PermissionsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Chen Keinan
 */
@Component("getEffectivePermission")
public class GetEffectivePermissionService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetEffectivePermissionService.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private RepositoryService repoService;

    @Autowired
    private UserGroupService userGroupService;

    private Multimap<String, String> userInGroups;

    public void execute(ArtifactoryRestRequest artifactoryRequest, RestResponse artifactoryResponse) {
        String path = artifactoryRequest.getQueryParamByKey("path");
        String repoKey = artifactoryRequest.getQueryParamByKey("repoKey");
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
        if (repoService.isVirtualRepoExist(repoPath.getRepoKey())) {
            repoPath = repoService.getVirtualItemInfo(repoPath).getRepoPath();
        }
        boolean canManage = authService.canManage(repoPath);
        if (canManage) {
            try {
                userInGroups = userGroupService.getAllUsersInGroups();
                artifactoryResponse.iModel(getPermissionModel(repoPath));
            } catch (Exception e) {
                log.debug("Artifactory was unable to get users and groups effective permissions, cause: ",
                        e.getMessage());
                artifactoryResponse
                        .error("Artifactory was unable to get users and groups effective permissions, see logs for more details.");
            }
        }
    }

    /**
     * Find and populate permission model from acl cache
     */
    private PermissionsModel getPermissionModel(RepoPath repoPath) {
        List<AclInfo> aclInfos = userGroupService.getRepoPathAcls(repoPath);
        Map<String, PathEffectivePermissions> userEffectivePermissions = Maps.newHashMap();
        Map<String, PathEffectivePermissions> groupEffectivePermissions = Maps.newHashMap();
        addAdminsToMaps(userEffectivePermissions, groupEffectivePermissions);
        aclInfos.forEach(aclInfo -> addAclInfoToAclList(aclInfo, userEffectivePermissions, groupEffectivePermissions));
        grantGroupUsersEffectivePermissions(groupEffectivePermissions, userEffectivePermissions);
        // update response with model
        return new PermissionsModel(userEffectivePermissions.values(), groupEffectivePermissions.values(), aclInfos);
    }

    /**
     * Add effective admins and groups to the permission model
     */
    private void addAdminsToMaps(Map<String, PathEffectivePermissions> userEffectivePermissions,
            Map<String, PathEffectivePermissions> groupEffectivePermissions) {
        Map<String, Boolean> allAdminUsers = userGroupService.getAllUsersAndAdminStatus(true);
        allAdminUsers.keySet().forEach(adminUser -> changeAdminPermissions(adminUser, userEffectivePermissions));
        List<String> allAdminGroups = userGroupService.getAllAdminGroupsNames();
        allAdminGroups.forEach(adminGroup -> changeAdminPermissions(adminGroup, groupEffectivePermissions));
    }

    /**
     * Give all the users of all the groups each group's permissions
     */
    private void grantGroupUsersEffectivePermissions(Map<String, PathEffectivePermissions> groupEffectivePermissions,
            Map<String, PathEffectivePermissions> userEffectivePermissions) {
        groupEffectivePermissions.entrySet().stream()
                .filter(grpToPath -> !grpToPath.getValue().isAdmin())
                .flatMap(this::groupPathToUserPath)
                .forEach(usrToPath -> copyPermissionsToUser(usrToPath.getRight(), usrToPath.getLeft(), userEffectivePermissions));
    }

    private Stream<Pair<String, PathEffectivePermissions>> groupPathToUserPath(
            Map.Entry<String, PathEffectivePermissions> grpToPath) {
        return userInGroups.get(grpToPath.getKey()).stream()
                .map(user -> Pair.of(user, grpToPath.getValue()));
    }

    /**
     * Copy permission from a group permission to user permissions
     *
     * @param groupPermissionsInfo     Group permission info to copy from
     * @param username                 User to copy permissions to
     * @param userEffectivePermissions User permissions map to add new permission info
     */
    private void copyPermissionsToUser(PathEffectivePermissions groupPermissionsInfo, String username,
            Map<String, PathEffectivePermissions> userEffectivePermissions) {
        userEffectivePermissions.putIfAbsent(username, new PathEffectivePermissions(username));
        PathEffectivePermissions userPermissionInfo = userEffectivePermissions.get(username);
        EffectivePermission effectivePermission = userPermissionInfo.getPermission();
        EffectivePermission groupPermission = groupPermissionsInfo.getPermission();
        effectivePermission.aggregatePermissions(groupPermission);
        addPermissionTargetsWithCap(userPermissionInfo, groupPermissionsInfo.getPermissionTargets());
        userPermissionInfo.setPermission(effectivePermission);
    }

    /**
     * Get all ACL ACEs and get their info
     */
    private void addAclInfoToAclList(AclInfo aclInfo, Map<String, PathEffectivePermissions> userEffectivePermissions,
            Map<String, PathEffectivePermissions> groupEffectivePermissions) {
        String permissionTargetName = aclInfo.getPermissionTarget().getName();
        aclInfo.getAces().forEach(aceInfo -> addAceInfo(aceInfo, permissionTargetName, userEffectivePermissions,
                groupEffectivePermissions));
    }

    /**
     * Add ace principal to the relevant map with his permissions
     */
    private void addAceInfo(AceInfo aceInfo, String permissionTargetName,
            Map<String, PathEffectivePermissions> userEffectivePermissions,
            Map<String, PathEffectivePermissions> groupEffectivePermissions) {
        if (aceInfo.isGroup()) {
            addPermissionsToPrincipal(aceInfo.getPrincipal(), permissionTargetName, aceInfo, groupEffectivePermissions);
        } else {
            addPermissionsToPrincipal(aceInfo.getPrincipal(), permissionTargetName, aceInfo, userEffectivePermissions);
        }
    }

    /**
     * Add the aceInfo permissions to the principal in the aceListInfo map.
     */
    private void addPermissionsToPrincipal(String principal, String permissionTargetName, AceInfo aceInfo,
            Map<String, PathEffectivePermissions> aceListInfo) {
        aceListInfo.putIfAbsent(principal, new PathEffectivePermissions(principal));
        PathEffectivePermissions pathEffectivePermissions = aceListInfo.get(principal);
        EffectivePermission effectivePermission = pathEffectivePermissions.getPermission();
        effectivePermission.aggregatePermissions(aceInfo);
        addPermissionTargetsWithCap(pathEffectivePermissions, permissionTargetName);
        aceListInfo.put(principal, pathEffectivePermissions);
    }

    /**
     * Add permission targets to permission info until cap has been reached
     */
    private void addPermissionTargetsWithCap(PathEffectivePermissions permissionInfo,
            List<String> permissionTargetsToAdd) {
        List<String> permissionTargets = permissionInfo.getPermissionTargets();
        for (String permissionToAdd : permissionTargetsToAdd) {
            if (!permissionInfo.isPermissionTargetsCap()) {
                permissionTargets.add(permissionToAdd);
                if (permissionInfo.isPermissionTargetsCap()) {
                    permissionInfo.setPermissionTargetsCap(true);
                    return;
                }
            }
        }
    }

    /**
     * Add permission target to permission info
     */
    private void addPermissionTargetsWithCap(PathEffectivePermissions permissionInfo, String permissionTargetToAdd) {
        permissionInfo.advancePermissionTargetsCount();
        if (!permissionInfo.isPermissionTargetsCap()) {
            permissionInfo.getPermissionTargets().add(permissionTargetToAdd);
            if (permissionInfo.isPermissionTargetsCap()) {
                permissionInfo.setPermissionTargetsCap(true);
            }
        }
    }

    /**
     * Change user/group permission to admin in the result map
     */
    private void changeAdminPermissions(String adminUser,
            Map<String, PathEffectivePermissions> aceListInfo) {
        aceListInfo.putIfAbsent(adminUser, new PathEffectivePermissions(adminUser));
        PathEffectivePermissions pathEffectivePermissions = aceListInfo.get(adminUser);
        EffectivePermission effectivePermission = pathEffectivePermissions.getPermission();
        effectivePermission.setRead(true);
        effectivePermission.setAnnotate(true);
        effectivePermission.setDeploy(true);
        effectivePermission.setDelete(true);
        pathEffectivePermissions.setAdmin(true);
    }

}

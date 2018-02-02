package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.permission;

import com.google.common.base.Strings;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AclInfo;
import org.artifactory.security.UserGroupInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author nadavy
 */
@Component("getEffectivePermissionByEntity")
public class GetEffectivePermissionsByEntityService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetEffectivePermissionsByEntityService.class);

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String repoKey = request.getQueryParamByKey("repoKey");
        String path = request.getQueryParamByKey("path");
        RepoPath repoPath = RepoPathFactory.create(repoKey, path);
        if (authorizationService.canManage(repoPath)) {
            EntityInfo entityInfo = getEntityInfo(request, repoPath);
            List<String> permissionsByEntity = getPermissionsByEntity(entityInfo);
            if (permissionsByEntity != null) {
                response.iModel(permissionsByEntity);
            }
        } else {
            response.responseCode(HttpServletResponse.SC_FORBIDDEN).buildResponse();
            log.error("Forbidden UI REST call from user " + authorizationService.currentUsername());
        }
    }

    private EntityInfo getEntityInfo(ArtifactoryRestRequest request, RepoPath repoPath) {
        EntityInfo entityInfo = new EntityInfo();
        entityInfo.repoPath = repoPath;
        entityInfo.entityName = request.getPathParamByKey("username");
        if (Strings.isNullOrEmpty(entityInfo.entityName)) {
            entityInfo.entityName = request.getPathParamByKey("groupname");
            entityInfo.isGroup = true;
        }
        return entityInfo;
    }

    /**
     * Returns all permissions names of a given entity
     */
    private List<String> getPermissionsByEntity(EntityInfo entityInfo) {
        List<AclInfo> repoPathAcls = userGroupService.getRepoPathAcls(entityInfo.repoPath);
        if (entityInfo.isGroup) {
            return repoPathAcls.stream().filter(acl -> entityNameInAce(acl, entityInfo.entityName, true))
                    .map(acl -> acl.getPermissionTarget().getName())
                    .collect(Collectors.toList());
        } else {
            try {
                Set<String> userGroups = getUserGroups(entityInfo);
                return repoPathAcls.stream().filter(acl -> userInAcl(acl, entityInfo.entityName, userGroups))
                        .map(acl -> acl.getPermissionTarget().getName())
                        .collect(Collectors.toList());
            } catch (UsernameNotFoundException e) {
                return null;
            }
        }

    }

    /**
     * Returns true if user or the user's group is in any of the ACL's ACEs, false otherwise
     */
    private boolean userInAcl(AclInfo acl, String userName, Set<String> groupNames) {
        return entityNameInAce(acl, userName, false) || userGroupInAce(acl, groupNames);
    }

    /**
     * Returns true if any ACL's ACE contains any of the user's group
     */
    private boolean userGroupInAce(AclInfo acl, Set<String> groupNames) {
        return acl.getAces().stream()
                .anyMatch(ace -> ace.isGroup() && groupNames.contains(ace.getPrincipal()));
    }

    /**
     * Returns true if the entity is in any of the ACL's ACEs, false otherwise
     */
    private boolean entityNameInAce(AclInfo acl, String userName, boolean isGroup) {
        return acl.getAces().stream()
                .anyMatch(ace -> ace.getPrincipal().equals(userName) && ace.isGroup() == isGroup);
    }

    private Set<String> getUserGroups(EntityInfo entityInfo) {
        return userGroupService.findUser(entityInfo.entityName)
                .getGroups()
                .stream()
                .map(UserGroupInfo::getGroupName)
                .collect(Collectors.toSet());
    }

    private class EntityInfo {
        boolean isGroup = false;
        String entityName;
        RepoPath repoPath;
    }
}

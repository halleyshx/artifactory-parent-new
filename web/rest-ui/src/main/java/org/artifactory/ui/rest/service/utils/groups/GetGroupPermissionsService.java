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

package org.artifactory.ui.rest.service.utils.groups;

import com.google.common.collect.Multimap;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AclService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AceInfo;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.ui.rest.model.admin.security.group.Group;
import org.artifactory.ui.rest.model.admin.security.user.UserPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chen Keian
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetGroupPermissionsService implements RestService<Group> {

    @Autowired
    private AclService aclService;

    @Override
    public void execute(ArtifactoryRestRequest<Group> request, RestResponse response) {
        Group group = request.getImodel();
        Multimap<PermissionTargetInfo, AceInfo> userPermissionByPrincipal = aclService.getGroupsPermissions(group.getGroups());

        List<UserPermissions> userPermissionsList = userPermissionByPrincipal.entries().parallelStream()
                .map(entry -> new UserPermissions(entry.getValue(), entry.getKey(), getRepoKeysSize(entry.getKey())))
                .collect(Collectors.toList());
        response.iModelList(userPermissionsList);
    }

    /**
     * get the number of repo keys size
     *
     * @param permission - permission target
     * @return
     */
    private int getRepoKeysSize(PermissionTargetInfo permission) {
        if (permission.getRepoKeys().contains("ANY")) {
            RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
            return repositoryService.getAllRepoKeys().size();
        } else if (permission.getRepoKeys().contains("ANY LOCAL")) {
            RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
            return repositoryService.getLocalRepoDescriptors().size();
        } else if (permission.getRepoKeys().contains("ANY REMOTE")) {
            RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
            return repositoryService.getCachedRepoDescriptors().size();
        }
        return 0;
    }
}

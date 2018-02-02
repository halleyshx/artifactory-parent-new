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

package org.artifactory.ui.rest.service.admin.security.permissions;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.permissions.AllUsersAndGroupsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chen keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAllUsersAndGroupsService implements RestService {

    @Autowired
    private UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<AllUsersAndGroupsModel.PrincipalInfo> allUsers = userGroupService.getAllUsersAndAdminStatus(false).entrySet()
                .stream()
                .map(entry -> new AllUsersAndGroupsModel.PrincipalInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<AllUsersAndGroupsModel.PrincipalInfo> allGroups = userGroupService.getAllGroups()
                .stream()
                .map(groupInfo -> new AllUsersAndGroupsModel.PrincipalInfo(groupInfo.getGroupName(), groupInfo.isAdminPrivileges()))
                .collect(Collectors.toList());

        AllUsersAndGroupsModel allUsersAndGroups = new AllUsersAndGroupsModel();
        allUsersAndGroups.setAllUsers(allUsers);
        allUsersAndGroups.setAllGroups(allGroups);
        response.iModel(allUsersAndGroups);
    }
}

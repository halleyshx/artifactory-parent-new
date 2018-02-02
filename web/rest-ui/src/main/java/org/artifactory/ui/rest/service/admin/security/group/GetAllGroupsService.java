/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.admin.security.group;

import org.artifactory.api.security.AclService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.security.GroupInfo;
import org.artifactory.ui.rest.common.SecurityModelPopulator;
import org.artifactory.ui.rest.model.admin.security.group.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAllGroupsService implements RestService {

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AclService aclService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<GroupInfo> groupInfos = userGroupService.getAllGroups();
        List<AclInfo> acls = aclService.getAllAcls();
        List<Group> groups = groupInfos.parallelStream()
                .map(SecurityModelPopulator::getGroupConfiguration)
                .map(group -> addGroupPermissions(group, acls))
                .collect(Collectors.toList());
        response.iModelList(groups);
    }

    private Group addGroupPermissions(Group group, List<AclInfo> acls) {
        group.setPermissions(acls.parallelStream()
                .filter(groupPermissionTargets(group.getGroupName()))
                .map(aclInfo -> aclInfo.getPermissionTarget().getName())
                .collect(Collectors.toList()));
        return group;
    }

    private Predicate<AclInfo> groupPermissionTargets(String groupName) {
        return acl -> acl.getAces().stream()
                .filter(AceInfo::isGroup)
                .filter(aceInfo -> groupName.equalsIgnoreCase(aceInfo.getPrincipal()))
                .findAny()
                .isPresent();
    }
}

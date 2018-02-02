/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.admin.security.group;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.GroupInfo;
import org.artifactory.ui.rest.common.SecurityModelPopulator;
import org.artifactory.ui.rest.model.admin.security.group.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Actually not just group names, but a leaner model that the user form consumes
 *
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAllGroupNamesService implements RestService {

    @Autowired
    private UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<GroupInfo> groupInfos = userGroupService.getAllGroups();
        List<Group> groupList = groupInfos.parallelStream()
                .map(SecurityModelPopulator::getGroupConfiguration)
                .collect(Collectors.toList());
        response.iModelList(groupList);
    }
}

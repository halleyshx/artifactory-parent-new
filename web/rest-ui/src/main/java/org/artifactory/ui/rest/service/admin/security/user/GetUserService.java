/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.admin.security.user;

import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.UserInfo;
import org.artifactory.ui.rest.common.SecurityModelPopulator;
import org.artifactory.ui.rest.model.admin.security.user.BaseUser;
import org.artifactory.ui.rest.model.admin.security.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import static org.artifactory.api.security.UserGroupService.UI_VIEW_BLOCKED_USER_PROP;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetUserService implements RestService {

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private CentralConfigService configService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String userName = request.getPathParamByKey("id");
        BaseUser user = getUser(userName);
        if (user == null) {
            response.error("No such user '" + userName + "'.").responseCode(HttpStatus.SC_NOT_FOUND);
        } else {
            response.iModel(user);
        }
    }

    private User getUser(String userName) {
        User user;
        UserInfo userInfo;
        try {
            userInfo = userGroupService.findUser(userName);
        } catch (UsernameNotFoundException e) {
            return null;
        }
        if (addonsManager.addonByType(CoreAddons.class).isAolAdmin(userInfo)) {
            return null;
        }
        user = SecurityModelPopulator.getUserConfiguration(userInfo);
        if (user.getLastLoggedInMillis() > 0) {
            user.setLastLoggedIn(configService.getDateFormatter().print(user.getLastLoggedInMillis()));
        }
        user.setDisableUIAccess(uiAccessDisabledForUser(userName));
        return user;
    }

    private boolean uiAccessDisabledForUser(String userName) {
        return Boolean.parseBoolean(userGroupService.findPropertiesForUser(userName).getFirst(UI_VIEW_BLOCKED_USER_PROP));
    }
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.common.service.admin.xray;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ClearAllXrayIndexTasksService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(IndexXrayService.class);

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        XrayAddon xrayAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(XrayAddon.class);
        if (xrayAddon.isXrayEnabled()) {
            xrayAddon.clearAllIndexTasks();
            response.info("Successfully triggered removal of all current Xray index tasks").responseCode(200);
        } else {
            String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
            String err = "Failing Xray index request received from " + remoteAddress + ": xray not enabled or not configured.";
            response.error(err).responseCode(400);
            log.debug(err);
        }

    }
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.common.service.admin.advance.sumologic;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.sumologic.SumoLogicConfigDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSumoLogicProxyService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(UpdateSumoLogicProxyService.class);

    @Autowired
    private CentralConfigService centralConfig;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            String proxy = request.getQueryParamByKey("proxy");
            MutableCentralConfigDescriptor mutableDescriptor = centralConfig.getMutableDescriptor();
            SumoLogicConfigDescriptor sumoLogicConfig = mutableDescriptor.getSumoLogicConfig();
            sumoLogicConfig.setProxy(centralConfig.getDescriptor().getProxy(proxy));
            centralConfig.saveEditedDescriptorAndReload(mutableDescriptor);
        } catch (Exception e) {
            String msg = "Error updating proxy: " + e.getMessage();
            log.error(msg, e);
            response.iModel(msg).responseCode(500);
        }
    }
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.common.service.admin.advance.sumologic;

import org.artifactory.logging.sumologic.SumoLogicService;
import org.artifactory.rest.common.model.sumologic.SumoLogicModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Yinon Avraham
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResetSumoLogicApplicationService implements RestService {

    @Autowired
    private SumoLogicService sumoLogicService;

    @Autowired
    private GetSumoLogicConfigService getSumoLogicConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        sumoLogicService.resetApplication(true);
        SumoLogicModel model = getSumoLogicConfigService.getSumoLogicModel(request);
        response.iModel(model);
    }
}

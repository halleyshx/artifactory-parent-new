/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.common.service.admin.xray;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
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
public class GetXrayLicenseService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetXrayLicenseService.class);

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String xrayLicense = addonsManager.getLicensePerProduct("xray");
        if (StringUtils.isBlank(xrayLicense)) {
            response.error("License does not have xray product").responseCode(400);
            log.debug("License does not have xray product");
        } else {
            byte[] decodedLicense = Base64.decodeBase64(xrayLicense);
            response.iModel(Base64.encodeBase64(decodedLicense));
        }
    }
}
/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.admin.security.signingkeys;

import org.artifactory.addon.common.gpg.GpgKeyStore;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveSigningKeyService implements RestService {

    @Autowired
    private GpgKeyStore gpgKeyStore;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        boolean isPublic = Boolean.valueOf(request.getQueryParamByKey("public"));
        removeKey(isPublic);
        response.info("Key was removed");
    }

    private void removeKey(boolean isPublic) {
        if (isPublic) {
            gpgKeyStore.removePublicKey();
        } else {
            gpgKeyStore.removePrivateKey();
        }
    }
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.admin.security.signingkeys;

import org.artifactory.addon.common.gpg.GpgKeyStore;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.signingkey.SignKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSigningKeyService implements RestService {

    @Autowired
    private GpgKeyStore gpgKeyStore;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        SignKey signKey = (SignKey) request.getImodel();
        String passPhrase = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), signKey.getPassPhrase());
        updatePassPhrase(response, passPhrase);
    }

    private void updatePassPhrase(RestResponse artifactoryResponse, String passPhrase) {
        gpgKeyStore.savePassPhrase(passPhrase);
        artifactoryResponse.info("Successfully updated signing pass-phrase");
    }
}

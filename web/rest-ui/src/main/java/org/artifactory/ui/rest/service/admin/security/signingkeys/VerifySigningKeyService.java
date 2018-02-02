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

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class VerifySigningKeyService implements RestService {

    @Autowired
    private GpgKeyStore gpgKeyStore;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        SignKey signKey = (SignKey) request.getImodel();
        String passPhrase = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), signKey.getPassPhrase());
        // verify signing key phrase
        verifySignKeyPhrase(response, passPhrase);
    }

    /**
     * verify signing key phrase
     *
     * @param artifactoryResponse - encapsulate data related to response
     * @param passPhrase          = sign key pass phrase
     */
    private void verifySignKeyPhrase(RestResponse artifactoryResponse, String passPhrase) {
        boolean hasRequisites = true;
        if (isEmpty(passPhrase)) {
            artifactoryResponse.warn("No pass-phrase supplied");
            hasRequisites = false;
        }
        if (!gpgKeyStore.hasPrivateKey()) {
            artifactoryResponse.error("No private key installed");
            hasRequisites = false;
        }
        if (!gpgKeyStore.hasPublicKey()) {
            artifactoryResponse.error("No public key installed");
            hasRequisites = false;
        }
        if (hasRequisites) {
            if (gpgKeyStore.verify(passPhrase)) {
                artifactoryResponse.info("Successfully verified keys and passphrase");
            } else {
                artifactoryResponse.error(
                        "Failed to sign and verify using the installed keys and supplied pass-phrase");
            }
        }
    }
}

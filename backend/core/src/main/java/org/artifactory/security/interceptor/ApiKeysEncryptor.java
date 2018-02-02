/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.artifactory.security.interceptor;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.Async;
import org.artifactory.security.props.auth.EncryptedTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
public class ApiKeysEncryptor {
    private static final Logger log = LoggerFactory.getLogger(ApiKeysEncryptor.class);

    @Async
    public void encryptOrDecryptAsynchronously(boolean encrypt) {
        encryptOrDecrypt(encrypt);
    }

    public void encryptOrDecrypt(boolean encrypt) {
        String message = encrypt ? "encryption" : "decryption";
        log.info("Starting {} of all users API Keys", message);
        ContextHelper.get().beansForType(EncryptedTokenManager.class)
                .forEach((s, manager) -> manager.encryptDecryptAllTokens(encrypt));
        log.info("Finished {} of all users API Keys", message);
    }
}

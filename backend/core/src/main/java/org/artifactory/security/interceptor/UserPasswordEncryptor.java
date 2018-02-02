/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.artifactory.security.interceptor;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.Async;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserInfo;
import org.jfrog.security.crypto.EncodedKeyPair;
import org.jfrog.security.crypto.EncryptionWrapper;
import org.jfrog.security.crypto.result.DecryptionStatusHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Shay Yaakov
 */
@Component
public class UserPasswordEncryptor {
    private static final Logger log = LoggerFactory.getLogger(UserPasswordEncryptor.class);

    @Autowired
    private UserGroupService userGroupService;

    @Async
    public void encryptOrDecryptAsynchronously(boolean encrypt) {
        encryptOrDecrypt(encrypt);
    }

    public void encryptOrDecrypt(boolean encrypt) {
        String message = encrypt ? "encryption" : "decryption";
        log.info("Starting {} of all users passwords", message);

        EncryptionWrapper masterWrapper = ArtifactoryHome.get().getArtifactoryEncryptionWrapper();
        List<UserInfo> allUsers = userGroupService.getAllUsers(true);
        for (UserInfo user : allUsers) {
            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
            if (encrypt) {
                mutableUser.setBintrayAuth(CryptoHelper.encryptIfNeeded(ArtifactoryHome.get(), user.getBintrayAuth()));
            } else {
                mutableUser.setBintrayAuth(CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), user.getBintrayAuth()));
            }
            if (StringUtils.isNotBlank(user.getPublicKey()) && StringUtils.isNotBlank(user.getPrivateKey())) {
                EncodedKeyPair encodedKeyPair = new EncodedKeyPair(user.getPrivateKey(), user.getPublicKey());
                EncryptionWrapper wrapper = encrypt ? masterWrapper : null;
                encodedKeyPair = new EncodedKeyPair(encodedKeyPair.decode(masterWrapper, new DecryptionStatusHolder()), wrapper);
                mutableUser.setPrivateKey(encodedKeyPair.getEncodedPrivateKey());
                mutableUser.setPublicKey(encodedKeyPair.getEncodedPublicKey());
            }
            userGroupService.updateUser(mutableUser, false, encrypt);
        }

        log.info("Finished {} of all users passwords", message);
    }
}

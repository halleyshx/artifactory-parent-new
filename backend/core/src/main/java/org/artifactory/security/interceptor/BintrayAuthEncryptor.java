package org.artifactory.security.interceptor;

import org.artifactory.api.repo.Async;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Inbar Tal
 */

@Component
public class BintrayAuthEncryptor {
    private static final Logger log = LoggerFactory.getLogger(UserPasswordEncryptor.class);

    @Autowired
    private UserGroupService userGroupService;

    @Async
    public void encryptOrDecryptAsynchronously(boolean encrypt) {
        encryptOrDecrypt(encrypt);
    }

    public void encryptOrDecrypt(boolean encrypt) {
        String message = encrypt ? "encryption" : "decryption";
        log.info("Starting {} of all bintray users", message);

        List<UserInfo> allUsers = userGroupService.getAllUsers(true);
        for (UserInfo user : allUsers) {
            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
            if (encrypt) {
                mutableUser.setBintrayAuth(CryptoHelper.encryptIfNeeded(ArtifactoryHome.get(), user.getBintrayAuth()));
            } else {
                mutableUser.setBintrayAuth(CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), user.getBintrayAuth()));
            }
            userGroupService.updateUser(mutableUser, false);
        }

        log.info("Finished {} of all bintray users", message);
    }
}

package org.artifactory.security.props.auth;

import java.util.Set;

/**
 * This interface is for managing user properties that require encryption/decryption.
 *
 * @author Rotem Kfir
 */
public interface EncryptedTokenManager {

    /**
     * @return The names of the properties that require encryption/decryption
     */
    Set<String> getPropKeys();

    /**
     * Encrypt or decrypt the properties for all users
     * @param encrypt true for encryption, false for decryption
     */
    void encryptDecryptAllTokens(boolean encrypt);
}

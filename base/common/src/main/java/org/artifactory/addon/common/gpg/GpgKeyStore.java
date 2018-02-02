/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.addon.common.gpg;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * @author Yoav Luft
 */
public interface GpgKeyStore {

    /**
     * @return The GPG signing public key file location (the file might not exist).
     */
    File getPublicKeyFile();

    /**
     * @return The ASCII Armored GPG signing key. Null if public key file doesn't exist
     * @throws IOException If failed to read the key from file
     */
    @Nullable
    String getPublicKey() throws IOException;

    void savePrivateKey(String privateKey) throws Exception;

    void savePublicKey(String publicKey) throws Exception;

    void savePassPhrase(String password);

    boolean hasPrivateKey();

    String getPrivateKey() throws IOException;

    void removePublicKey();

    void removePrivateKey();

    boolean verify(String passphrase);

    boolean hasPublicKey();
}

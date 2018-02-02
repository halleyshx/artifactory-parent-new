/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.descriptor.security.signingkeys;

/**
 * @author Gidi Shabat
 */

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "SigningKeysSettingsType",
        propOrder = {"passphrase","keyStorePassword"},
        namespace = Descriptor.NS)
@GenerateDiffFunction
public class SigningKeysSettings implements Descriptor {

    private String passphrase;

    private String keyStorePassword;

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SigningKeysSettings that = (SigningKeysSettings) o;

        if (passphrase != null ? !passphrase.equals(that.passphrase) : that.passphrase != null) {
            return false;
        }
        return (keyStorePassword != null ? keyStorePassword.equals(that.keyStorePassword) : that.keyStorePassword == null);
    }

    @Override
    public int hashCode() {
        int result = passphrase != null ? passphrase.hashCode() : 0;
        result = 31 * result + (keyStorePassword != null ? keyStorePassword.hashCode() : 0);
        return result;
    }
}

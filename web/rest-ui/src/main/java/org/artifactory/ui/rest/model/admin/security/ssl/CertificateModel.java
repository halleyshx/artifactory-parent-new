package org.artifactory.ui.rest.model.admin.security.ssl;

import org.artifactory.rest.common.model.RestModel;

/**
 * @author Shay Bagants
 */
public class CertificateModel implements RestModel {
    private String certificateName;
    private String certificatePEM;

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }
}

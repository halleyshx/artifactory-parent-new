package org.artifactory.ui.rest.model.admin.security.ssl;

import java.util.List;

/**
 * @author Shay Bagants
 */
public class CertificateDeleteModel {

    private List<String> certificates;

    public List<String> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<String> certificates) {
        this.certificates = certificates;
    }
}

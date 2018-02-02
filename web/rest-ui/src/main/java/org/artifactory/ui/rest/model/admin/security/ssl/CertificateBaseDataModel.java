package org.artifactory.ui.rest.model.admin.security.ssl;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Shay Bagants
 */
public class CertificateBaseDataModel {

    private String alias;
    @JsonProperty("issued_to")
    private String issuedTo;
    @JsonProperty("issued_by")
    private String issuedBy;
    private String fingerprint;
    @JsonProperty("valid_until")
    private String validUntil;

    public CertificateBaseDataModel(String alias, String issuedTo, String issuedBy, String fingerprint, String valitUntil) {
        this.alias = alias;
        this.issuedTo = issuedTo;
        this.issuedBy = issuedBy;
        this.fingerprint = fingerprint;
        this.validUntil = valitUntil;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
}


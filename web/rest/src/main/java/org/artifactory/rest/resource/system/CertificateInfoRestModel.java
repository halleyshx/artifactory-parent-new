package org.artifactory.rest.resource.system;

/**
 * Model representing a certificate information returned by the REST-API
 *
 * @author Shay Bagants
 */
public class CertificateInfoRestModel {

    public CertificateInfoRestModel() {
    }

    public CertificateInfoRestModel(String certificateAlias, String issuedTo, String issuedBy, String issuedOn,
            String validUntil, String fingerprint) {
        this.certificateAlias = certificateAlias;
        this.issuedTo = issuedTo;
        this.issuedBy = issuedBy;
        this.issuedOn = issuedOn;
        this.validUntil = validUntil;
        this.fingerprint = fingerprint;
    }

    private String certificateAlias;
    private String issuedTo;
    private String issuedBy;
    private String issuedOn;
    private String validUntil;
    private String fingerprint;

    public String getCertificateAlias() {
        return certificateAlias;
    }

    public void setCertificateAlias(String certificateAlias) {
        this.certificateAlias = certificateAlias;
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

    public String getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(String issuedOn) {
        this.issuedOn = issuedOn;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}

package org.artifactory.addon.license;

/**
 * Internal model that should be used to retrieve the license details from the AddonManager for HA.
 * This is not a REST-API model!
 *
 * @author Shay Bagants
 */
public class ArtifactoryHaLicenseDetails extends ArtifactoryBaseLicenseDetails {

    private String licenseHash;
    private Boolean expired;
    private String nodeId;
    private String nodeUrl;

    public ArtifactoryHaLicenseDetails(String type, String validThrough, String licensedTo,
            String licenseHash, Boolean expired, String nodeId, String nodeUrl) {
        super(type, validThrough, licensedTo);
        this.licenseHash = licenseHash;
        this.expired = expired;
        this.nodeId = nodeId;
        this.nodeUrl = nodeUrl;
    }

    public String getLicenseHash() {
        return licenseHash;
    }

    public void setLicenseHash(String licenseHash) {
        this.licenseHash = licenseHash;
    }

    public Boolean isExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeUrl() {
        return nodeUrl;
    }

    public void setNodeUrl(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }
}

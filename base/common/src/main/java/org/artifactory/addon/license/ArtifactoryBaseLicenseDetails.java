package org.artifactory.addon.license;

/**
 * Internal model that should be used to retrieve the license details from the AddonManager for Pro and Trial.
 * This is not a REST-API model!
 *
 * @author Shay Bagants
 */
public class ArtifactoryBaseLicenseDetails {

    private String type;
    private String validThrough;
    private String licensedTo;

    public ArtifactoryBaseLicenseDetails() {
    }

    public ArtifactoryBaseLicenseDetails(String type, String validThrough, String licensedTo) {
        this.type = type;
        this.validThrough = validThrough;
        this.licensedTo = licensedTo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(String validThrough) {
        this.validThrough = validThrough;
    }

    public String getLicensedTo() {
        return licensedTo;
    }

    public void setLicensedTo(String licensedTo) {
        this.licensedTo = licensedTo;
    }
}

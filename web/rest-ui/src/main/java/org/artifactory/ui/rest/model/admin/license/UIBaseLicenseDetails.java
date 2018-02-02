package org.artifactory.ui.rest.model.admin.license;

import org.artifactory.rest.common.model.BaseModel;

/**
 * UI REST model for basic license details
 *
 * @author Shay Bagants
 */
public class UIBaseLicenseDetails extends BaseModel {

    private String type;
    private String validThrough;
    private String licensedTo;
    private String licenseKey;

    public UIBaseLicenseDetails(String type, String validThrough, String licensedTo) {
        this.type = type;
        this.validThrough = validThrough;
        this.licensedTo = licensedTo;
    }

    @SuppressWarnings("UnusedDeclaration")
    public UIBaseLicenseDetails() {
        // used to de-serialize
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

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
}

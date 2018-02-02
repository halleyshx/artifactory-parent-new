package org.artifactory.ui.rest.model.admin.license;


import org.artifactory.rest.common.model.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * UI REST model represent Artifactory cluster licenses details
 *
 * @author Shay Bagants
 */
public class UILicensesDetails extends BaseModel {

    private List<UILicenseFullDetails> licenses = new ArrayList<>();
    private int nodesWithNoLicense = 0;

    public List<UILicenseFullDetails> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<UILicenseFullDetails> licenses) {
        this.licenses = licenses;
    }

    public int getNodesWithNoLicense() {
        return nodesWithNoLicense;
    }

    public void setNodesWithNoLicense(int nodesWithNoLicense) {
        this.nodesWithNoLicense = nodesWithNoLicense;
    }

    public static class UILicenseFullDetails extends UIBaseLicenseDetails {

        public UILicenseFullDetails(String type, String validThrough, String licensedTo, String licenseHash,
                boolean isExpired, String nodeId, String nodeUrl) {
            super(type, validThrough, licensedTo);
            this.licenseHash = licenseHash;
            this.isExpired = isExpired;
            this.nodeId = nodeId;
            this.nodeUrl = nodeUrl;
        }

        public UILicenseFullDetails() {

        }

        private String licenseHash;
        private Boolean isExpired;
        private String nodeId;
        private String nodeUrl;

        public String getLicenseHash() {
            return licenseHash;
        }

        public void setLicenseHash(String licenseHash) {
            this.licenseHash = licenseHash;
        }

        public boolean isExpired() {
            return isExpired;
        }

        public void setExpired(Boolean expired) {
            isExpired = expired;
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
}

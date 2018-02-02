package org.artifactory.rest.common.model.artifactorylicense;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Model represent Artifactory cluster licenses details
 *
 * @author Shay Bagants
 */
public class LicensesDetails {

    @JsonProperty("licenses")
    private List<LicenseFullDetails> licenses = new ArrayList<>();

    public List<LicenseFullDetails> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<LicenseFullDetails> licenses) {
        this.licenses = licenses;
    }

    public static class LicenseFullDetails extends BaseLicenseDetails {

        public LicenseFullDetails(String type, String validThrough, String licensedTo, String licenseHash,
                boolean isExpired, String nodeId, String nodeUrl) {
            super(type, validThrough, licensedTo);
            this.licenseHash = licenseHash;
            this.isExpired = isExpired;
            this.nodeId = nodeId;
            this.nodeUrl = nodeUrl;
        }

        public LicenseFullDetails() {

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

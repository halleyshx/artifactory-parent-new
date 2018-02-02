package org.artifactory.rest.common.model.artifactorylicense;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shay Bagants
 */
public class RemoveClusterLicenseModel {

    @JsonProperty("licenses")
    private List<RemoveLicenseModel> licenses = new ArrayList<>();

    public List<RemoveLicenseModel> getLicenses() {
        return licenses;
    }

    public void setLicenses(
            List<RemoveLicenseModel> licenses) {
        this.licenses = licenses;
    }

    public static class RemoveLicenseModel {

        private String licenseHash;

        public String getLicenseHash() {
            return licenseHash;
        }

        public void setLicenseHash(String licenseHash) {
            this.licenseHash = licenseHash;
        }
    }
}

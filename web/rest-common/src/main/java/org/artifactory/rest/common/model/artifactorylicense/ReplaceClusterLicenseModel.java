package org.artifactory.rest.common.model.artifactorylicense;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shay Bagants
 */
public class ReplaceClusterLicenseModel {

    @JsonProperty("licenses")
    private List<ReplaceLicenseModel> licenses = new ArrayList<>();

    @JsonProperty("newLicenses")
    private List<ReplaceLicenseModel> newLicenses = new ArrayList<>();

    public static class ReplaceLicenseModel {

        private String licenseHash;

        public String getLicenseHash() {
            return licenseHash;
        }

        public void setLicenseHash(String licenseHash) {
            this.licenseHash = licenseHash;
        }
    }
}
package org.artifactory.rest.common.model.artifactorylicense;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * This model is for internal usages. Activate license, with ignoring a specific license provided in the model
 *
 * @author Shay Bagants
 */
public class ActivateClusterLicenseModel {

    @JsonProperty("ignoredLicenses")
    private Set<IgnoredLicense> ignoredLicenses = new HashSet<>();

    public ActivateClusterLicenseModel(Set<IgnoredLicense> ignoredLicenses) {
        this.ignoredLicenses = ignoredLicenses;
    }

    public ActivateClusterLicenseModel() {
    }

    public static ActivateClusterLicenseModel buildModel(Set<String> ignoredLicenses) {
        ActivateClusterLicenseModel model = new ActivateClusterLicenseModel();
        ignoredLicenses.forEach(ignoredLicense -> {
            model.getIgnoredLicenses().add(new IgnoredLicense(ignoredLicense));
        });
        return model;
    }

    public Set<IgnoredLicense> getIgnoredLicenses() {
        return ignoredLicenses;
    }

    public void setIgnoredLicenses(Set<IgnoredLicense> ignoredLicenses) {
        this.ignoredLicenses = ignoredLicenses;
    }

    public static class IgnoredLicense {

        private String licenseHash;

        public IgnoredLicense(String licenseHash) {
            this.licenseHash = licenseHash;
        }

        public IgnoredLicense() {
        }

        public String getLicenseHash() {
            return licenseHash;
        }

        public void setLicenseHash(String licenseHash) {
            this.licenseHash = licenseHash;
        }
    }
}

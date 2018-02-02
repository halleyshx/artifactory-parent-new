package org.artifactory.rest.resource.system;

import java.util.HashSet;
import java.util.Set;

/**
 * REST-API model to add multiple licenses
 *
 * @author Shay Bagants
 */
public class LicensesConfiguration {

    private Set<LicenseConfiguration> licenses = new HashSet<>();

    public LicensesConfiguration() {
    }

    public Set<LicenseConfiguration> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<LicenseConfiguration> licenses) {
        this.licenses = licenses;
    }
}

package org.artifactory.rest.common.model.artifactorylicense;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Model that represent one or more licenses.
 * <pre>
 * {@code
 * {
 *      "licenses" : [
 *          {
 *              "licenseKey" : "abcd"
 *          },{
 *              "licenseKey" : "efgh"
 *          }
 *      ]
 * }
 * }
 * </pre>
 *
 * @author Shay Bagants
 */
public class AddClusterLicenseModel {

    @JsonProperty("licenses")
    private List<BaseLicenseDetails> licenses = new ArrayList<>();

    public List<BaseLicenseDetails> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<BaseLicenseDetails> licenses) {
        this.licenses = licenses;
    }

    public void addLicense(BaseLicenseDetails licenseDetails) {this.licenses.add(licenseDetails);}

    /**
     * Model that represent single license.
     */
    public static class AddLicenseModel {

        private String licenseKey;

        public String getLicenseKey() {
            return licenseKey;
        }

        public void setLicenseKey(String licenseKey) {
            this.licenseKey = licenseKey;
        }
    }
}

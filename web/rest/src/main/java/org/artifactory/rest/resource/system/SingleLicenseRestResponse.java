package org.artifactory.rest.resource.system;

/**
 * Single response message for an Artifactory license operation
 *
 * @author Shay Bagants
 */
public class SingleLicenseRestResponse {

    private int status = 200;
    private String message;

    public SingleLicenseRestResponse() {
    }

    public SingleLicenseRestResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

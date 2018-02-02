package org.artifactory.addon.ha.propagation.supportbundle;

import org.apache.http.HttpStatus;

/**
 * @author Dan Feldman
 */
public class SupportBundleDeleteResponse {

    private final int statusCode;
    private final String message;

    public SupportBundleDeleteResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return statusCode != HttpStatus.SC_OK;
    }
}

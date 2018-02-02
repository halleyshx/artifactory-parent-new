package org.artifactory.rest.resource.token;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * See <a href="https://tools.ietf.org/html/rfc6749#section-5.2">RFC6749 - Section 5.2</a>
 * @author Yinon Avraham
 */
public class TokenResponseErrorModel {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}

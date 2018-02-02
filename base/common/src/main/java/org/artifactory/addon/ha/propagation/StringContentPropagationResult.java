package org.artifactory.addon.ha.propagation;

import javax.annotation.Nullable;

/**
 * @author Yinon Avraham
 */
public class StringContentPropagationResult implements PropagationResult<String> {

    private final int statusCode;
    private final String errorMessage;
    private final String content;

    public StringContentPropagationResult(int statusCode, String content, String errorMessage) {
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.content = content;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getContent() {
        return content;
    }
}

package org.artifactory.addon.ha.propagation.uideploy;

import org.artifactory.addon.ha.propagation.PropagationResult;

import javax.annotation.Nullable;

/**
 * @author Dan Feldman
 * @author Yinon Avraham
 */
public class UIDeployPropagationResult implements PropagationResult<String> {

    private final int statusCode;
    private final String errorMessage;
    private final String content;

    public UIDeployPropagationResult(int statusCode, String content, String errorMessage) {
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

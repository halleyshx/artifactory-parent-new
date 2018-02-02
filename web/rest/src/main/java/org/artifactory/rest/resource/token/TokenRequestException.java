package org.artifactory.rest.resource.token;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * @author Yinon Avraham
 */
public class TokenRequestException extends WebApplicationException {

    public TokenRequestException(@Nonnull TokenResponseErrorCode errorCode, @Nullable String errorDescription) {
        super(createResponse(errorCode, errorDescription));
    }

    public TokenRequestException(@Nonnull TokenResponseErrorCode errorCode, @Nonnull Throwable cause) {
        super(cause, createResponse(errorCode, cause.getMessage()));
    }

    private static Response createResponse(TokenResponseErrorCode errorCode, String errorDescription) {
        TokenResponseErrorModel model = new TokenResponseErrorModel();
        model.setError(errorCode.getErrorMessage());
        model.setErrorDescription(errorDescription);
        return Response
                .status(errorCode.getResponseCode())
                .entity(model)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }
}

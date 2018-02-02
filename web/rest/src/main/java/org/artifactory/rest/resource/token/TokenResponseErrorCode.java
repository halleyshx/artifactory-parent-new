package org.artifactory.rest.resource.token;

/**
 * See <a href="https://tools.ietf.org/html/rfc6749#section-5.2">RFC6749 - Section 5.2</a>
 * @author Yinon Avraham
 */
public enum TokenResponseErrorCode {

    /**
     * The request is missing a required parameter, includes an unsupported parameter value (other than grant type),
     * repeats a parameter, includes multiple credentials, utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    InvalidRequest("invalid_request", 400),
    /**
     * Client authentication failed (e.g., unknown client, no client authentication included, or unsupported
     * authentication method).
     */
    InvalidClient("invalid_client", 401),
    /**
     * The provided authorization grant (e.g., authorization code, resource owner credentials) or refresh token is
     * invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to
     * another client.
     */
    InvalidGrant("invalid_grant", 401),
    /**
     * The authenticated client is not authorized to use this authorization grant type.
     */
    UnauthorizedClient("unauthorized_client", 400),
    /**
     * The authorization grant type is not supported by the authorization server.
     */
    UnsupportedGrantType("unsupported_grant_type", 400),
    /**
     * The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner.
     */
    InvalidScope("invalid_scope", 400),
    /**
     * The requested repopath in repopath scope is not found
     */
    InvalidRepoPath("invalid_repo_path", 404);

    private final String errorMessage;
    private final int responseCode;

    TokenResponseErrorCode(String errorMessage, int responseCode) {
        this.errorMessage = errorMessage;
        this.responseCode = responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }
}

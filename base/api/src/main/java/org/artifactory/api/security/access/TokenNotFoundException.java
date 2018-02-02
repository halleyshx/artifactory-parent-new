package org.artifactory.api.security.access;

/**
 * @author Yinon Avraham
 */
public class TokenNotFoundException extends RuntimeException {

    public TokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenNotFoundException(String message) {
        super(message);
    }
}

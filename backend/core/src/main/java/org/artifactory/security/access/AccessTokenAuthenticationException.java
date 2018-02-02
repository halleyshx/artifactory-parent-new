package org.artifactory.security.access;

import org.springframework.security.core.AuthenticationException;

/**
 * @author Yinon Avraham
 */
public class AccessTokenAuthenticationException extends AuthenticationException {

    public AccessTokenAuthenticationException(String message) {
        super(message);
    }

    public AccessTokenAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

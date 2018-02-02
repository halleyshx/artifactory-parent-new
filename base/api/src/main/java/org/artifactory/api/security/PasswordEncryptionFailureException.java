package org.artifactory.api.security;

/**
 * @author Shay Bagants
 */
public class PasswordEncryptionFailureException extends RuntimeException {

    public PasswordEncryptionFailureException(String message) {
        super(message);
    }
}

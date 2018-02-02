package org.artifactory.exception;

/**
 * @author Shay Bagants
 */
public class InvalidCertificateException extends Exception {

    public InvalidCertificateException() {
    }

    public InvalidCertificateException(String message) {
        super(message);
    }

    public InvalidCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}

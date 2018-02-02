package org.artifactory.config;

/**
 * Specific exception for operations that failed (unsupported) on old versions of Xray
 *
 * @author Shay Bagants
 */
public class XrayOperationException extends Exception {

    public XrayOperationException(String message) {
        super(message);
    }
}

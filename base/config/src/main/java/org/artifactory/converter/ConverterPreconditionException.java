package org.artifactory.converter;

/**
 * Exception of converter prerequisite not satisfied, having to fail Artifactory on startup
 * {@see RTFACT-14343}
 *
 * @author nadavy
 */
public class ConverterPreconditionException extends RuntimeException {

    public ConverterPreconditionException(String message) {
        super(message);
    }
}

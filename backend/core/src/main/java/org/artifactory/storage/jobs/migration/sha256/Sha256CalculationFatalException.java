package org.artifactory.storage.jobs.migration.sha256;

/**
 * This exception denotes a fatal, unrecoverable error that deems all related nodes as failures that must be resolved
 * by user intervention
 *
 * @author Dan Feldman
 */
public class Sha256CalculationFatalException extends Exception {

    Sha256CalculationFatalException(String message) {
        super(message);
    }

    Sha256CalculationFatalException(String message, Throwable cause) {
        super(message, cause);
    }
}

package org.artifactory.exception;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * @author gidis
 */
public class SQLIntegrityException extends RuntimeException {
    public SQLIntegrityException(String message, SQLIntegrityConstraintViolationException e) {
        super(message,e);
    }
}

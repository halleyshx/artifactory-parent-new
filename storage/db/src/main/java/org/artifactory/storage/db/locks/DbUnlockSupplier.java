package org.artifactory.storage.db.locks;

import java.sql.SQLException;

/**
 * Used to perform DB unlock and force-unlock operations.
 * Similar to {@link java.util.function.BooleanSupplier}, but throws SQLException
 *
 * @author Shay Bagants
 */
@FunctionalInterface
public interface DbUnlockSupplier {

    /**
     * Perform DB unlock operation.
     *
     * @return true if unlock operation succeeded, false otherwise
     * @throws SQLException if an error occurred when communicating against the DB
     */
    boolean unlock() throws SQLException;
}

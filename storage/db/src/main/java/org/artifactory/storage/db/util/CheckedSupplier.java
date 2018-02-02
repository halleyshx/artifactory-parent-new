package org.artifactory.storage.db.util;

/**
 * @author Noam Shemesh
 * @param <T> Type of return value
 * @param <E> Type of the expected checked exception
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception> {
    T get() throws E;
}
package org.artifactory.storage.db.locks.service;

import org.artifactory.spring.ReloadableBean;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author gidis
 */
public interface DbLocksService extends ReloadableBean {

    /**
     * Clean DB locks which are expired
     */
    void cleanDbExpiredLocks();

    /**
     * Clean cached locks which are expired
     */
    void cleanCachedExpiredLocks();

    /**
     * Try to acquire lock on combination of category and key. Throws {@link TimeoutException} if lock could not be
     * acquired within the specified timeout.
     *
     * @param category the lock category
     * @param key      the lock key
     * @param owner    the owner instance name
     * @param timeout  the time to wait for the lock
     * @param timeUnit the time unit of the timeout argument
     * @throws TimeoutException if the waiting time elapsed before acquiring a lock on the key-category combination
     */
    void acquireLock(String category, String key, String owner, long timeout, TimeUnit timeUnit)
            throws TimeoutException;

    /**
     * Check is a combination of category and key is locked
     */
    boolean isLocked(String category, String key);

    /**
     * Check a combination of category and key is locked in the cache by the current thread
     */
    boolean isLockedByMe(String category, String key);

    boolean unlock(String category, String key, String owner);

    int lockingMapSize(String category);

    Set<String> lockingMapKeySet(String category);

    boolean forceUnlock(String category, String key);
}

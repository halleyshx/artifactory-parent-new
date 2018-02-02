package org.artifactory.storage.db.security.service;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gidi Shabat
 */
public class VersioningCache<T extends BasicCacheModel> {
    private static final Logger log = LoggerFactory.getLogger(VersioningCache.class);

    private ReentrantLock cacheLock = new ReentrantLock();
    private long timeout;
    private final Callable<T> cacheLoader;
    // promoted on each DB change (permission change/add/delete)
    private AtomicInteger dbVersion = new AtomicInteger(1);
    private volatile int version = 0; // promoted each time we load the cache from DB
    private volatile T cache;

    public VersioningCache(long timeout, Callable<T> cacheLoader) {
        this.timeout = timeout;
        this.cacheLoader = cacheLoader;
    }

    /**
     * Call this method each permission update/change/delete in DB.
     */
    public int promoteDbVersion() {
        return dbVersion.incrementAndGet();
    }

    /**
     * Returns cache.
     */
    public T get() {
        T currentCache = cache;
        if (dbVersion.get() > version) {
            // Need to update cache (new version in dbVersion).
            // Try to acquire acl lock
            log.debug("Attempting to acquire a lock on cacheLock");
            boolean lockAcquired = tryToWaitForLock();
            if (!lockAcquired) {
                // Timeout occurred : Return the current cache without waiting to thew new cache which is being reloaded.
                log.debug("cache lock timeout occurred returning current cache instead the one that is being loaded");
                return cache;
            } else {
                log.debug("cacheLock lock has been acquired");
                // Lock was successfully acquired, now we can start reloading the cache
                try {
                    currentCache = cache;
                    //print only if debug is enabled to avoid performances degradation on large cache
                    if (log.isDebugEnabled()) {
                        log.debug("Current cache : " + currentCache);
                    }
                    // Double check after cacheLoader synchronization.
                    if (dbVersion.get() > version) {
                        log.debug("aclsDbVersion version '{}' is higher than version: {}", dbVersion.get(), version);
                        // The map will be valid for version the current aclsDbVersion.
                        int startingVersion = dbVersion.get();
                        try {
                            currentCache = cacheLoader.call();
                        } catch (Exception e) {
                            throw new VersioningCacheException("Fail to reload cache:", e);
                        }
                        T oldCache = cache;
                        cache = currentCache;
                        if (oldCache != null) {
                            oldCache.destroy();
                        }
                        //print only if debug is enabled to avoid performances degradation on large cache
                        if (log.isDebugEnabled()) {
                            log.debug("current cache has been updated with: " + currentCache);
                        }
                        currentCache.setVersion(startingVersion);
                        version = startingVersion;
                    } else {
                        log.debug("Skipping cache update, newer version exist: dbVersion is: '{}'" +
                                " while version version is: {}", dbVersion.get(), version);
                    }
                } finally {
                    cacheLock.unlock();
                }
            }
        }
        return currentCache;
    }

    private boolean tryToWaitForLock() {
        boolean acquireLock = false;
        try {
            acquireLock = cacheLock.tryLock(timeout, TimeUnit.MILLISECONDS);
            if (!acquireLock && cache == null) {
                log.debug("Blocking thread while cache is being processed for the first time");
                acquireLock = tryToWaitForLock();
            }
        } catch (InterruptedException e) {
            if (cache == null) {
                log.debug("Blocking thread while cache is being processed for the first time");
                acquireLock = tryToWaitForLock();
            }
        }
        return acquireLock;
    }
}
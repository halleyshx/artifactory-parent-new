package org.artifactory.storage.db.locks.service;

import org.jfrog.storage.common.ConflictGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author gidis
 */
public class DBConflictGuard implements ConflictGuard {
    private static final Logger log = LoggerFactory.getLogger(DBConflictGuard.class);

    private DbLocksService dbLocksService;
    private String key;
    private String category;
    private String serverId;

    public DBConflictGuard(DbLocksService dbLocksService, String key, String category, String serverId) {
        this.dbLocksService = dbLocksService;
        this.key = key;
        this.category = category;
        this.serverId = serverId;
    }

    @Override
    public boolean tryToLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            dbLocksService.acquireLock(category, key, serverId, timeout, timeUnit);
            return true;
        } catch (TimeoutException e) {
            log.warn("Timed out while trying to acquire lock: {}:{}. {}", category, key, e.getMessage());
            log.debug("Timed out while trying to acquire lock", e);
            return false;
        }
    }

    @Override
    public void unlock() {
        dbLocksService.unlock(category, key, serverId);
    }

    @Override
    public void forceUnlock() {
        dbLocksService.unlock(category, key, serverId);
    }

    @Override
    public boolean isLocked() {
        return dbLocksService.isLocked(category, key);
    }

}

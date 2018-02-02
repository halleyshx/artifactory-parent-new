package org.artifactory.storage.db.locks.service;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.locks.DbUnlockSupplier;
import org.artifactory.storage.db.locks.LockInfo;
import org.artifactory.storage.db.locks.dao.DbDistributeLocksDao;
import org.artifactory.storage.fs.lock.LockingDebugUtils;
import org.artifactory.util.CollectionUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.artifactory.storage.db.locks.service.HaDbLockHelper.*;

/**
 * @author gidis
 */
@Service
@Reloadable(beanClass = DbLocksService.class, initAfter = DbService.class)
public class DbLocksServiceImpl implements DbLocksService {
    private static final Logger log = LoggerFactory.getLogger(DbLocksServiceImpl.class);

    private final DbDistributeLocksDao dbDistributeLocksDao;
    private final Map<String, LockInfo> localLocks = new ConcurrentHashMap<>();

    @Autowired
    public DbLocksServiceImpl(DbDistributeLocksDao dbDistributeLocksDao) {
        this.dbDistributeLocksDao = dbDistributeLocksDao;
    }

    public void acquireLock(String category, String key, String owner, long timeout, TimeUnit timeUnit) throws TimeoutException {
        timeout = timeUnit.toMillis(timeout);
        long start = System.currentTimeMillis();
        log.debug("Acquiring lock for: " + getLockInfo(category, key, owner));
        long startTime = System.currentTimeMillis();
        long sleepTime = 8;
        long waitingTime;
        LockInfo lockInfo = new LockInfo(category, key, owner, Thread.currentThread().getId(), Thread.currentThread().getName(), start);
        while (true) {
            try {
                // Assume key is not locked
                log.trace("Attempting to acquire lock for: " + getLockInfo(category, key, owner));
                // we update the time on the model before inserting into the db, because in case we wait for x time
                // until we were able to acquire the lock, we don't want it's expiry to be shorter.
                lockInfo.setStartedTime(System.currentTimeMillis());
                if (dbDistributeLocksDao.tryToAcquireLock(lockInfo)) {
                    localLocks.put(toLocalId(category, key), lockInfo);
                    log.trace("Successfully acquired lock: '{}'.", lockInfo);
                    long timeTookToAcquireLock = System.currentTimeMillis() - start;
                    log.debug("Lock acquired in '{}' milliseconds.", timeTookToAcquireLock);
                    return;
                } else {
                    LockInfo dbLockInfo = dbDistributeLocksDao.getLockInfo(category, key);
                    if (dbLockInfo != null && dbLockInfo.getThreadId() == Thread.currentThread().getId() && dbLockInfo.getOwner().equals(owner)) {
                        throw new RuntimeException("Reentrant lock is not supported");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to acquire lock for: " + lockInfo, e);
                throw new RuntimeException("Failed to acquire lock for: " + getLockInfo(category, key, owner), e);
            }
            // If we got here the lock exist but we don't own it
            long currentTime = System.currentTimeMillis();
            waitingTime = currentTime - startTime;
            waitLimitedTime(timeout, sleepTime, waitingTime);
            sleepTime = Math.min(sleepTime * 4, 2048);
            log.trace("Waiting while trying to acquiring lock for: " + getLockInfo(category, key, owner));
        }
    }

    public boolean isLocked(String category, String key) {
        log.debug("Checking if lock exist for: " + getLockInfo(category, key));
        try {
            if (localLocks.containsKey(toLocalId(category, key))) {
                log.trace("Lock exist locally for: " + getLockInfo(category, key));
                return true;
            }
            if (dbDistributeLocksDao.isLocked(category, key)) {
                log.trace("Lock exist for: " + getLockInfo(category, key));
                return true;
            }
            log.trace("Lock doesn't exist for: " + getLockInfo(category, key));
            return false;
        } catch (SQLException e) {
            log.debug("Failed to check if lock exist for:" + getLockInfo(category, key), e);
            throw new RuntimeException("Failed to check if lock exist for:" + getLockInfo(category, key), e);
        }
    }

    @Override
    public boolean isLockedByMe(String category, String key) {
        LockInfo lockInfo = localLocks.get(toLocalId(category, key));
        return lockInfo != null && Thread.currentThread().getId() == lockInfo.getThreadId();
    }


    public boolean unlock(String category, String key, String owner) {
        log.debug("Attempting to release lock for: " + getLockInfo(category, key));
        try {
            // Assume lock acquired by this thread and try to update lock
            log.trace("Attempting to release lock from cache for: " + getLockInfo(category, key, owner));
            LockInfo lockInfo = localLocks.get(toLocalId(category, key));
            if (lockInfo != null) {
                if (lockInfo.getThreadId() == Thread.currentThread().getId()) {
                    localLocks.remove(toLocalId(category, key));
                    log.trace("Successfully delete lock from cache for: " + lockInfo);
                } else {
                    String msg = "Failed to release lock (inconsistent state) for: {}. Current thread is not the " +
                            "owner of the lock." + lockInfo;
                    StringBuilder messageBuilder = new StringBuilder().append(msg);
                    LockingDebugUtils.debugLocking(messageBuilder);
                    throw new RuntimeException(msg);
                }
            }
            DbUnlockSupplier unlockSupplier = buildUnlockSupplier(category, key, owner);
            boolean dbLockRemoval = unlockInternal(unlockSupplier, category, key, owner, 3);
            if (dbLockRemoval) {
                log.trace("Successfully delete lock from cache for: " + getLockInfo(category, key, owner));
                return true;
            } else {
                log.error("Could not remove {} lock.", getLockInfo(category, key, owner));
            }
        } catch (Exception e) {
            log.error("Failed to release lock for:" + getLockInfo(category, key, owner), e.getMessage());
            log.debug("Failed to release lock for:" + getLockInfo(category, key, owner), e);
            throw new RuntimeException("Failed to release lock for: " + getLockInfo(category, key, owner), e);
        }
        log.debug("Lock is either not exist or not owned by current owner for: " + getLockInfo(category, key));
        return false;
    }

    /**
     * Unlock combination of 'category', 'key' and optinally 'owner' from the DB with retries and short sleep between each retry
     *
     * @param numOfRetries Num of times to re-try in case that the first attempt failed
     * @throws SQLException In
     */
    private boolean unlockInternal(DbUnlockSupplier unlockSupplier, String category, String key, String owner,
            int numOfRetries) throws SQLException {
        // basically, we need retry when catching an exception only, however, although it does not makes sense without DB cluster, we saw once that a lock was exists in Oracle DB, and the unlock operation returned false.
        boolean success = false;
        int sleepTime = 4; // sleep time in ms
        while (numOfRetries >= 0) {
            try {
                success = unlockSupplier.unlock();
            } catch (SQLException e) {
                log.trace("SQL Error occurred while trying to delete lock.", e);
                if (numOfRetries == 0) {
                    throw e;
                }
            }
            if (success) {
                return true;
            }
            log.debug("Failed removing lock for {}", getLockInfo(category, key, owner));
            if (numOfRetries > 0) {
                // sleep, no more than half a second
                sleepTime = Math.min(sleepTime, 256);
                sleep(sleepTime);
                sleepTime = sleepTime * 4;
            }
            numOfRetries--;
        }
        return false;
    }

    public int lockingMapSize(String category) {
        log.debug("Querying for category map size for: " + getLockInfo(category, null));
        try {
            int numberOfLocks = dbDistributeLocksDao.lockingMapSize(category);
            log.trace("Found " + numberOfLocks + " locks for:" + getLockInfo(category));
            return numberOfLocks;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query out number of locks for: " + getLockInfo(category));
        }
    }

    public Set<String> lockingMapKeySet(String category) {
        log.debug("Querying for category map size for: " + getLockInfo(category));
        try {
            Set<String> set = dbDistributeLocksDao.lockingMapKeySet(category);
            log.trace("Found the following locks " + set + "  for:" + getLockInfo(category));
            return set;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query out locks for:" + getLockInfo(category));
        }
    }

    public boolean forceUnlock(String category, String key) {
        log.debug("Releasing lock for: " + getLockInfo(category));
        try {
            LockInfo removed = localLocks.remove(toLocalId(category, key));
            DbUnlockSupplier forceUnlockSupplier = getForceUnlockSupplier(category, key);
            boolean dbLockRemoved = unlockInternal(forceUnlockSupplier, category, key, null, 3);
            return removed != null || dbLockRemoved;
        } catch (SQLException e) {
            throw new RuntimeException("" +
                    "Failed to force lock release for: " + getLockInfo(category, key));
        }
    }

    @Override
    public void init() {
        String serverId = ContextHelper.get().getServerId();
        try {
            int effectedRows = dbDistributeLocksDao.deleteAllOwnerLocks(serverId);
            log.debug("{} rows were removed during initialization.", effectedRows);
        } catch (SQLException e) {
            log.warn("Failed deleting old locks during initialization. {}", e.getMessage());
            log.debug("Failed deleting old locks during initialization.", e);
        }
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {

    }

    public void cleanDbExpiredLocks() {
        log.debug("Attempting to release DB expired locks.");
        Set<LockInfo> locks;
        try {
            long current = System.currentTimeMillis();
            log.trace("Current time is: {}", current);
            long minAllowedTime = current - TimeUnit.MINUTES.toMillis(ConstantValues.hazelcastMaxLockLeaseTime.getLong());
            locks = dbDistributeLocksDao.getExpiredLocks(minAllowedTime);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list locks.", e);
        }
        if (CollectionUtils.notNullOrEmpty(locks)) {
            log.debug("Cleaning '{}' expired locks", locks.size());
            forceRemoveLocks(locks, false);
        }
    }

    @Override
    public void cleanCachedExpiredLocks() {
        log.debug("Attempting to release cached expired locks.");
        Set<LockInfo> locks;
        long current = System.currentTimeMillis();
        log.trace("Current time is: {}", current);
        long minAllowedTime = current - TimeUnit.MINUTES.toMillis(ConstantValues.hazelcastMaxLockLeaseTime.getLong());
        locks = localLocks.values().stream()
                .filter(Objects::nonNull)
                .filter(lockInfo -> lockInfo.getStartedTime() < minAllowedTime)
                .collect(Collectors.toSet());
        if (CollectionUtils.notNullOrEmpty(locks)) {
            log.debug("Cleaning cached '{}' expired locks", locks.size());
            forceRemoveLocks(locks, true);
        }
    }

    @Override
    public void destroy() {
        String owner = ContextHelper.get().getServerId();
        log.debug("Destroying all locks for server: {} ", owner);
        try {
            Set<LockInfo> locks = dbDistributeLocksDao.getAllCurrentServerLocks(owner);
            forceRemoveLocks(locks, false);
        } catch (SQLException e) {
            throw new RuntimeException("" +
                    "Failed to destroy locks for server: " + owner);
        }
    }

    /**
     * @param locks            List of locks to release, should not be null or empty.
     * @param removeCachedOnly Whether to release the locks from the cache only, or from both DB and cache
     */
    private void forceRemoveLocks(@Nonnull Set<LockInfo> locks, boolean removeCachedOnly) {
        for (LockInfo lock : locks) {
            log.debug("Destroying lock for category: {}, key: {} ", lock.getCategory(), lock.getKey());
            localLocks.remove(toLocalId(lock.getCategory(), lock.getKey()));
            if (!removeCachedOnly) {
                try {
                    DbUnlockSupplier unlockSupplier = getForceUnlockSupplier(lock.getCategory(), lock.getKey());
                    unlockInternal(unlockSupplier, lock.getCategory(), lock.getKey(), null, 1);
                } catch (Exception e) {
                    // the lock might have already deleted, so we ignore.
                    log.debug("Failed to destroy lock for category: {}, key: {} . {}", lock.getCategory(),
                            lock.getKey(), e.getMessage());
                    log.trace("Failed to destroy lock.", e);
                }
            }
        }
    }

    private DbUnlockSupplier getForceUnlockSupplier(String category, String key) {
        return () -> dbDistributeLocksDao.releaseForceLock(category, key);
    }

    private DbUnlockSupplier buildUnlockSupplier(String category, String key, String owner) {
        return () -> dbDistributeLocksDao.deleteLock(category, key, owner);
    }

    private String toLocalId(String category, String key) {
        return "category:" + category + ",key:" + key;
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }
}

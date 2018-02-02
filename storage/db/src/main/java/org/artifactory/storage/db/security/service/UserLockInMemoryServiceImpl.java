package org.artifactory.storage.db.security.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.security.UserInfo;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.security.service.UserLockInMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Noam Shemesh
 */
@Component
public class UserLockInMemoryServiceImpl implements UserLockInMemoryService {
    private static final Logger log = LoggerFactory.getLogger(UserLockInMemoryServiceImpl.class);

    public static final int MAX_USERS_TO_TRACK = 10000; // max locked users to keep in cache
    private static final int MAX_LOGIN_DELAY = 5000;
    private static final int OK_INCORRECT_LOGINS = 2; // delay will start after OK_INCORRECT_LOGINS+1 attempts
    private final int loginDelayMultiplier = getLoginDelayMultiplier();

    /**
     * Calculates user login delay multiplier,
     * the value (security.loginBlockDelay) is
     * taken from system properties file,
     * <p/>
     * delay may not exceed
     *
     * @return user login delay multiplier
     */
    private int getLoginDelayMultiplier() {
        int userDefinedDelayMultiplier = ConstantValues.loginBlockDelay.getInt();
        if (userDefinedDelayMultiplier <= MAX_LOGIN_DELAY) {
            return userDefinedDelayMultiplier;
        }
        log.warn("loginBlockDelay '{}' has exceeded maximum allowed delay '{}', " +
                                "which will be used instead", userDefinedDelayMultiplier, MAX_LOGIN_DELAY);
        return MAX_LOGIN_DELAY;
    }

    private static final boolean CACHE_BLOCKED_USERS = ConstantValues.useFrontCacheForBlockedUsers.getBoolean();

    // cache meaning  <username, lock-time>
    private final Cache<String, Long> userAccessUsersCache = CacheBuilder.newBuilder().maximumSize(MAX_USERS_TO_TRACK).
            expireAfterWrite(24, TimeUnit.HOURS).build();

    private final Cache<String, Long> lockedUsersCache = CacheBuilder.newBuilder().maximumSize(MAX_USERS_TO_TRACK).
            expireAfterWrite(24, TimeUnit.HOURS).build();

    private final Map<String, AtomicInteger> incorrectLoginAttemptsCache = Maps.newConcurrentMap();

    @Override
    public void updateUserAccess(String username, boolean userLockPolicyEnabled, long accessTimeMillis) {
        if (StringUtils.isNotBlank(username) && !userLockPolicyEnabled && !UserInfo.ANONYMOUS.equals(username)) {
            if (!isUserLocked(username)) {
                userAccessUsersCache.put(username, accessTimeMillis);
            }
        }
    }

    @Override
    public void lockUser(@Nonnull String username) {
        try {
            synchronized (lockedUsersCache) {
                registerLockedOutUser(username);
            }
        } catch (Exception e) {
            log.debug("Could not lock user {}, cause: {}", username, e);
            throw new StorageException("Could not lock user " + username + ", reason: " + e.getMessage());
        }
    }

    @Override
    public boolean isUserLocked(String username) {
        if (shouldCacheLockedUsers()) {
            if (lockedUsersCache.getIfPresent(username) != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public long getNextLoginDelay(String userName) {
        Long lastAccessTime = userAccessUsersCache.getIfPresent(userName);
        if (lastAccessTime != null) {
            return getNextLoginDelay(
                    getIncorrectLoginAttempts(userName),
                    lastAccessTime
            );
        }
        return -1;
    }

    @Override
    public long getNextLoginDelay(int incorrectLoginAttempts, long lastAccessTimeMillis) {
        if (incorrectLoginAttempts >= OK_INCORRECT_LOGINS) {
            long delay = (long) ((incorrectLoginAttempts - OK_INCORRECT_LOGINS) * loginDelayMultiplier);
            if (delay != 0) {
                return lastAccessTimeMillis + (delay <= MAX_LOGIN_DELAY ? delay : MAX_LOGIN_DELAY);
            }
        }
        return -1;
    }

    @Override
    public void unlockUser(@Nonnull String username) {
        try {
            synchronized (lockedUsersCache) {
                unregisterLockedOutUser(username);
            }
        } catch (Exception e) {
            log.debug("Could not unlock user {}, cause: {}", username, e);
            throw new StorageException("Could not unlock user " + username + ", reason: " + e.getMessage());

        }
    }

    @Override
    public void unlockAllUsers() {
        lockedUsersCache.invalidateAll();
    }

    //TODO: [by YS] consider moving this to Access client

    /**
     * Registers locked out user in cache
     */
    private void registerLockedOutUser(String username) {
        if (shouldCacheLockedUsers()) {
            lockedUsersCache.put(username, System.currentTimeMillis());
        }
    }

    /**
     * @return whether locked out users should be cached
     */
    private boolean shouldCacheLockedUsers() {
        return CACHE_BLOCKED_USERS;
    }

    /**
     * Unregisters locked out user/s from cache
     *
     * @param user a user name to unlock or all users via ALL_USERS
     *             {@see UserGroupServiceImpl.ALL_USERS}
     */
    private void unregisterLockedOutUser(String user) {
        if (shouldCacheLockedUsers()) {
            lockedUsersCache.invalidate(user);
        }
    }

    @Override
    public int getIncorrectLoginAttempts(@Nonnull String userName) {
        AtomicInteger incorrectLoginAttempts = incorrectLoginAttemptsCache.get(userName);
        return incorrectLoginAttempts != null ? incorrectLoginAttempts.get() : 0;
    }

    @Override
    public void registerIncorrectLoginAttempt(@Nonnull String userName) {
        AtomicInteger atomicInteger = incorrectLoginAttemptsCache.get(userName);
        if (atomicInteger == null) {
            synchronized (incorrectLoginAttemptsCache) {
                atomicInteger = incorrectLoginAttemptsCache.get(userName);
                if (atomicInteger == null) {
                    atomicInteger = new AtomicInteger(0);
                    incorrectLoginAttemptsCache.put(userName, atomicInteger);
                }
            }
        }
        int incorrectLoginAttempt = atomicInteger.incrementAndGet();
        log.debug("Increased IncorrectLoginAttempts for user '{}' to '{}'", userName, incorrectLoginAttempt);
    }

    @Override
    public void resetIncorrectLoginAttempts(@Nonnull String userName) {
        if (incorrectLoginAttemptsCache.containsKey(userName)) {
            synchronized (incorrectLoginAttemptsCache) {
                incorrectLoginAttemptsCache.remove(userName);
            }
            log.debug("Reset IncorrectLoginAttempts for user '{}'", userName);
        }
    }
}

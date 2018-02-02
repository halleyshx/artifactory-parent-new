package org.artifactory.addon.security;

import com.google.common.collect.Maps;
import org.artifactory.addon.LockingProvider;
import org.artifactory.common.ConstantValues;
import org.artifactory.storage.fs.lock.FsItemsVault;
import org.artifactory.storage.fs.lock.FsItemsVaultCacheImpl;
import org.artifactory.storage.fs.lock.provider.JVMLockProvider;
import org.artifactory.storage.fs.lock.provider.JvmConflictsGuard;
import org.jfrog.storage.common.ConflictGuard;
import org.jfrog.storage.common.ConflictsGuard;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author gidis
 */
public class JvmConflictGuardProvider implements LockingProvider {
    private final FsItemsVault vault;
    private Map<String, ConflictsGuard> map;
    private ReentrantLock lock;

    public JvmConflictGuardProvider() {
        map = Maps.newHashMap();
        lock = new ReentrantLock();
        JVMLockProvider jvmLockProvider = new JVMLockProvider();
        vault = new FsItemsVaultCacheImpl(jvmLockProvider);
    }

    @Override
    public FsItemsVault getFolderLockingMap() {
        return vault;
    }

    @Override
    public FsItemsVault getFileLockingMap() {
        return vault;
    }

    @Override
    public ConflictGuard getConflictGuard(String key) {
        ConflictsGuard conflictsGuard = map.get("general");
        if (conflictsGuard == null) {
            lock.lock();
            try {
                conflictsGuard = map.get("general");
                if (conflictsGuard == null) {
                    conflictsGuard = new JvmConflictsGuard(ConstantValues.hazelcastMaxLockLeaseTime.getLong());
                    map.put(key, conflictsGuard);
                }
            } finally {
                lock.unlock();
            }
        }
        return conflictsGuard.getLock(key);
    }

    @Override
    public <T> ConflictsGuard<T> getConflictsGuard(String key) {
        if ("general".equals(key)) {
            throw new RuntimeException("The 'general' map is reserved for internal usage");
        }
        ConflictsGuard<T> conflictsGuard = (ConflictsGuard<T>) map.get(key);
        if (conflictsGuard == null) {
            lock.lock();
            try {
                conflictsGuard = (ConflictsGuard<T>) map.get(key);
                if (conflictsGuard == null) {
                    conflictsGuard = new JvmConflictsGuard<>(ConstantValues.hazelcastMaxLockLeaseTime.getLong());
                    map.put(key, conflictsGuard);
                }
            } finally {
                lock.unlock();
            }
        }
        return conflictsGuard;
    }
}

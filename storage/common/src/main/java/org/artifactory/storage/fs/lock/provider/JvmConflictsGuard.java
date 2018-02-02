package org.artifactory.storage.fs.lock.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import org.artifactory.storage.fs.lock.MonitoringReentrantLock;
import org.jfrog.storage.common.ConflictGuard;
import org.jfrog.storage.common.ConflictsGuard;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Fred Simon on 9/7/16.
 */
public class JvmConflictsGuard<K> implements ConflictsGuard<K> {

    private LoadingCache<K, LockWrapper> locks;

    public JvmConflictsGuard(long timeout) {
        locks = CacheBuilder.newBuilder().initialCapacity(2000).softValues()
                .expireAfterAccess(timeout, TimeUnit.MINUTES)
                .build(new CacheLoader<K, LockWrapper>() {
                    @Override
                    public LockWrapper load(@Nonnull K key) throws Exception {
                        return new JVMLockWrapper(new MonitoringReentrantLock());
                    }
                });
    }

    @Override
    public void unlock(K key) {
        try {
            getLockInternal(key).unlock();
        } catch (IllegalMonitorStateException e) {
            // This exception might thrown when the lock in the cache was already removed and the getLockInternal method forces the cache to build a new lockWrapper object
            throw new RuntimeException("Failed to release lock for key: " + key, e);
        }
    }

    @Override
    public void forceUnlock(K key) {
        unlock(key);
    }

    private LockWrapper getLockInternal(K key) {
        try {
            return locks.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to retrieve lock on key: " + key, e);
        }
    }

    @Override
    public boolean tryToLock(K key, long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        return getLockInternal(key).tryLock(timeout, timeoutUnit);
    }

    @Override
    public int size() {
        int count = 0;
        for (LockWrapper lockWrapper : locks.asMap().values()) {
            if (lockWrapper.isLocked()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isLocked(K key) {
        return getLockInternal(key).isLocked();
    }

    @Override
    public Set<K> keySet() {
        return Sets.newHashSet();
    }

    @Override
    public ConflictGuard getLock(K key) {
        return new JvmConflictGuard(getLockInternal(key));
    }
}

package org.artifactory.storage.fs.lock.provider;


import org.jfrog.storage.common.ConflictGuard;
import java.util.concurrent.TimeUnit;

/**
 * @author gidis
 */
public class JvmConflictGuard implements ConflictGuard {


    private final LockWrapper lockWrapper;

    public JvmConflictGuard(LockWrapper lockWrapper) {
        this.lockWrapper = lockWrapper;
    }

    @Override
    public boolean tryToLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            return lockWrapper.tryLock(timeout,timeUnit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void unlock() {
        lockWrapper.unlock();
    }

    @Override
    public void forceUnlock() {
        lockWrapper.forceUnlock();
    }

    @Override
    public boolean isLocked() {
        return lockWrapper.isLocked();
    }

}

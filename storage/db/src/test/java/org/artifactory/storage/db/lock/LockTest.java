package org.artifactory.storage.db.lock;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Lists;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.db.locks.provider.DbMapLockWrapper;
import org.artifactory.storage.db.locks.service.DbLocksServiceImpl;
import org.artifactory.storage.fs.lock.FsItemsVaultCacheImpl;
import org.artifactory.storage.fs.lock.LockEntryId;
import org.artifactory.storage.fs.lock.provider.LockWrapper;
import org.artifactory.test.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.*;

//TODO [by shayb]: I've added DbLocksDaoTest, but we still need similar test to the actual service, therefore, this needs to be converted to DbLockServiceTest and we should add all the service functionallities  to this test
/**
 * @author gidis
 */
public class LockTest extends DbBaseTest {
    private static final Logger log = LoggerFactory.getLogger(LockTest.class);

    @Autowired
    private DbLocksServiceImpl dbLocksService;

    @BeforeClass
    public void setup() {
        importSql("/sql/aql_test.sql");
        TestUtils.setLoggingLevel(DbMapLockWrapper.class, Level.ERROR);
    }

    // Perform concurrent try lock operations on the DB locking mechanism and make sure
    @Test
    public void concurrentDBLockTest() throws TimeoutException, ExecutionException, InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(40, 100, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        FsItemsVaultCacheImpl vaultCache = new FsItemsVaultCacheImpl(repoPath -> new DbMapLockWrapper(repoPath, dbLocksService, "server"));
        List<Future> futures = Lists.newArrayList();
        for (int i = 0; i < 200; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 500; j++) {
                    LockEntryId lock = vaultCache.getLock(RepoPathFactory.create("repo", "test"));
                    LockWrapper lockWrapper = lock.getLock();
                    if (lockWrapper.isHeldByCurrentThread()) {
                        throw new RuntimeException("Current thread should not be the owner of a lock!");
                    }
                    if (lockWrapper.tryLock(1, TimeUnit.MILLISECONDS)) {
                        try {
                            Thread.sleep(100);
                            if (!lockWrapper.isHeldByCurrentThread()) {
                                throw new RuntimeException("Current thread must be owner of this lock!");
                            }
                        } finally {
                            lockWrapper.unlock();
                        }
                        if (lockWrapper.isHeldByCurrentThread()) {
                            throw new RuntimeException("Current thread should not be the owner of a lock!");
                        }
                    }

                }
                return true;
            }));
        }

        for (Future future : futures) {
            future.get();
        }
    }
}

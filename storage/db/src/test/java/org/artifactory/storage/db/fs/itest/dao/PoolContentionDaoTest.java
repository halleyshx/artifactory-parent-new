package org.artifactory.storage.db.fs.itest.dao;

import org.artifactory.storage.db.fs.dao.NodesDao;
import org.artifactory.storage.db.fs.entity.Node;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal test for loading the database with lots of artificial requests.
 *
 * @author Yossi Shaul
 */
@Test(enabled = false)
public class PoolContentionDaoTest extends DbBaseTest {

    @Autowired
    private NodesDao nodesDao;
    private final int nThreads = 1000;
    private final int maxTestTime = 1200000;

    @BeforeClass
    public void setup() {
        importSql("/sql/nodes.sql");
    }

    public void pool() throws InterruptedException {
        long start = System.currentTimeMillis();
        final AtomicLong queries = new AtomicLong(0);
        for (int i = 0; i < nThreads; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        Node node = nodesDao.get(4);
                        queries.incrementAndGet();
                        if (queries.get() % 10000 == 0) {
                            logTimes(start, queries);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        Thread.sleep(maxTestTime);
    }

    public void noPool() throws InterruptedException {
        long start = System.currentTimeMillis();
        final AtomicLong queries = new AtomicLong(0);
        for (int i = 0; i < nThreads; i++) {
            new Thread(() -> {
                try {
                    Connection c = (Connection) ReflectionTestUtils.invokeGetterMethod(jdbcHelper, "getConnection");
                    PreparedStatement pstmt = c.prepareStatement("SELECT * FROM nodes WHERE node_id = 4");
                    while (true) {
                        ResultSet resultSet = pstmt.executeQuery();
                        resultSet.close();
                        //pstmt.close();
                        queries.incrementAndGet();
                        if (queries.get() % 10000 == 0) {
                            logTimes(start, queries);
                        }
                        //Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(maxTestTime);
    }

    private void logTimes(long start, AtomicLong queries) {
        long timePassed = System.currentTimeMillis() - start;
        double averageTime = (double) timePassed / queries.get();
        long qps = (queries.get() * 1000) / timePassed;
        System.out.println(queries.get() + " qps: " + qps + " avg: " + averageTime + " ms");
    }
}
package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * Test DB version v104 (art version v420 -> v431)
 *
 * @author Dan Feldman
 */
@Test
public class V104UpgradeTest extends UpgradeBaseTest {

    private final static String STATS_REMOTE_TABLE = "stats_remote";

    public void testV104Conversion() throws IOException, SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "node_id"));
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "origin"));
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "download_count"));
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "last_downloaded"));
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "last_downloaded_by"));
            assertTrue(DbTestUtils.isColumnExist(con, STATS_REMOTE_TABLE, "path"));
        }
    }
}

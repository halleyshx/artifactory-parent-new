package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * Test DB version v107 (art version v440)
 *
 * @author Dan Feldman
 */
@Test
public class V107UpgradeTest extends UpgradeBaseTest {

    public void testV107Conversion() throws IOException, SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(con, "users", "locked"));
        }
    }
}

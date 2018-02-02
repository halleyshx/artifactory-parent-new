package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * Test DB version v201 (art version v530)
 *
 * @author Dan Feldman
 */
@Test
public class V201UpgradeTest extends UpgradeBaseTest {

    public void testV201Conversion() throws IOException, SQLException {
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(connection, "groups", "admin_privileges"));
        }
    }
}

package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * Test DB version v108 (art version v441 -> v4141)
 *
 * @author Dan Feldman
 */
@Test
public class V108UpgradeTest extends UpgradeBaseTest {

    public void testV108Conversion() throws IOException, SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(con, "users", "credentials_expired"));
        }
    }
}

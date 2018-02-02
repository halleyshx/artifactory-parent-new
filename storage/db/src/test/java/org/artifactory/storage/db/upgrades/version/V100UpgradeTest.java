package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertFalse;

/**
 * Test DB version v100 (art version v300)
 *
 * @author Shay Yaakov
 */
@Test
public class V100UpgradeTest extends UpgradeBaseTest {

    public void test300DBChanges() throws IOException, SQLException {
        // Now the DB is like in 3.0.x, should be missing the new tables of 3.1.x
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            Assert.assertTrue(DbTestUtils.isTableMissing(connection));
        }
    }
}

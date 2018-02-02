package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * Test DB version v200 (art version v500)
 *
 * @author Yossi Shaul
 */
@Test
public class V200UpgradeTest extends UpgradeBaseTest {

    public void test500DBChanges() throws IOException, SQLException {
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(connection, "tasks", "created"));
        }
    }
}

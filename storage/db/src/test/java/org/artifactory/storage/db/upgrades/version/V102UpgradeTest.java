package org.artifactory.storage.db.upgrades.version;

import org.apache.commons.lang.RandomStringUtils;
import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.jfrog.storage.DbType;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertEquals;

/**
 * Test DB version v102 (art version v311 -> v402)
 *
 * @author Yoav Luft
 */
@Test
public class V102UpgradeTest extends UpgradeBaseTest {

    public void test311DBChanges() throws IOException, SQLException {
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            assertEquals(DbTestUtils.getColumnSize(connection, "node_props", "prop_value"), 4000);
            if (dbProperties.getDbType() == DbType.MSSQL) {
                return; // RTFACT-5768
            }
            jdbcHelper.executeUpdate("INSERT INTO node_props VALUES(?, ?, ?, ?)",
                    15, 15, "longProp", RandomStringUtils.randomAscii(3999));
        }
    }
}

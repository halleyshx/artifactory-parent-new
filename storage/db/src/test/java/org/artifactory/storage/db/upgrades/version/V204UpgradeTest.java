package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbUtils;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * @author nadavy
 */
@Test
public class V204UpgradeTest extends UpgradeBaseTest {

    public void testV204NodePropsIndices() throws SQLException {
            // added indices
            assertTrue(DbUtils.indexExists(jdbcHelper, "node_props", "node_id",
                    "node_props_node_prop_value_idx", dbProperties.getDbType()));
            assertTrue(DbUtils.indexExists(jdbcHelper, "node_props", "prop_value",
                    "node_props_node_prop_value_idx", dbProperties.getDbType()));
            assertTrue(DbUtils.indexExists(jdbcHelper, "node_props", "prop_key",
                    "node_props_node_prop_value_idx", dbProperties.getDbType()));
            assertTrue(DbUtils.indexExists(jdbcHelper, "node_props", "prop_key",
                    "node_props_prop_key_value_idx", dbProperties.getDbType()));
            assertTrue(DbUtils.indexExists(jdbcHelper, "node_props", "prop_value",
                    "node_props_prop_key_value_idx", dbProperties.getDbType()));

            // removed indices
            if (!dbProperties.getDbType().equals(DbType.DERBY)) {
                // derby doesn't allow deleting index of primary key
                assertTrue(DbTestUtils.indexNotExists(jdbcHelper, "node_props", "node_id",
                        "node_props_node_id_idx", dbProperties.getDbType()));
            }
            assertTrue(DbTestUtils.indexNotExists(jdbcHelper, "node_props", "prop_key",
                    "node_props_prop_key_idx", dbProperties.getDbType()));
            assertTrue(DbTestUtils.indexNotExists(jdbcHelper, "node_props", "prop_value",
                    "node_props_prop_value_idx", dbProperties.getDbType()));
    }
}

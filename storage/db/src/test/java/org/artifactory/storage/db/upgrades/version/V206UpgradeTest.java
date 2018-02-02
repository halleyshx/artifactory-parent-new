package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.jfrog.storage.util.DbUtils.indexExists;
import static org.testng.Assert.assertTrue;

/**
 * @author Dan Feldman
 */
@Test
public class V206UpgradeTest extends UpgradeBaseTest {

    public void testSha256Index() throws SQLException {
        // added indices
        assertTrue(indexExists(jdbcHelper, "binaries", "sha256", "binaries_sha256_idx", dbProperties.getDbType()));
    }
}

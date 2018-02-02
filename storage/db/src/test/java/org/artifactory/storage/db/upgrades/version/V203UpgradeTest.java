package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.artifactory.storage.db.version.converter.DBSqlConverter;
import org.jfrog.storage.util.DbUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.*;

import static org.jfrog.storage.util.DbUtils.normalizedName;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test DB version v203 (art version v550)
 *
 * @author Dan Feldman
 */
@Test
public class V203UpgradeTest extends UpgradeBaseTest {

    //v550(+a,b,c) conversion adds the sha256 column to nodes and binaries tables.
    public void testV203Sha2Conversion() throws IOException, SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(con, "binaries", "sha256"));
            assertTrue(isColumnNullable(con, "nodes", "sha256"), "sha256 column should be nullable after v550 conversion");
            assertTrue(DbTestUtils.isColumnExist(con, "nodes", "sha256"));
            assertTrue(DbUtils.indexExists(jdbcHelper, "nodes", "sha256", "nodes_sha256_idx", dbProperties.getDbType()));
        }
    }

    //v550d adds the not null constraint to the binaries table.
    @Test(dependsOnMethods = {"testV203Sha2Conversion"})
    public void testV203Sha2MigrationConversion() throws SQLException, IOException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            //kombina to allow setting the not null constraint on the sha2 col
            jdbcHelper.executeUpdate("UPDATE binaries SET sha256 = '1' WHERE sha256 IS NULL");
            new DBSqlConverter("v550d").convert(jdbcHelper, dbProperties.getDbType());
            assertFalse(isColumnNullable(con, "binaries", "sha256"), "Expected v550d conversion to pass.");
        }
    }

    //v550(+a,b,c) conversion adds the repo_path_checksum column to the nodes table and the distributed_locks table
    @Test(dependsOnMethods = {"testV203Sha2MigrationConversion"})
    public void testV203NoHazelcastConversion() throws IOException, SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            assertTrue(DbTestUtils.isColumnExist(con, "nodes", "repo_path_checksum"));
            assertTrue(isColumnNullable(con, "nodes", "repo_path_checksum"));
            assertTrue(DbTestUtils.tableExists(con, "distributed_locks"));
            assertTrue(DbTestUtils.isColumnExist(con, "distributed_locks", "category"));
            assertTrue(DbTestUtils.isColumnExist(con, "distributed_locks", "lock_key"));
            assertTrue(DbTestUtils.isColumnExist(con, "distributed_locks", "owner"));
            assertTrue(DbTestUtils.isColumnExist(con, "distributed_locks", "owner_thread"));
            assertTrue(DbTestUtils.isColumnExist(con, "distributed_locks", "acquire_time"));
            assertTrue(DbUtils.indexExists(jdbcHelper, "distributed_locks", "owner", "distributed_locks_owner", dbProperties.getDbType()));
            assertTrue(DbUtils.indexExists(jdbcHelper, "distributed_locks", "owner_thread", "distributed_locks_owner_thread", dbProperties.getDbType()));
        }
    }

    //v550e adds the 'nodes_repo_path_checksum' index on the repo_path_checksum column in nodes table
    @Test(dependsOnMethods = {"testV203NoHazelcastConversion"})
    public void testV203NoHazelcastMigrationConversion() throws SQLException, IOException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            makeChecksumColumnUnique(con);
            new DBSqlConverter("v550e").convert(jdbcHelper, dbProperties.getDbType());
            assertTrue(DbUtils.indexExists(jdbcHelper, "nodes", "repo_path_checksum", "nodes_repo_path_checksum", dbProperties.getDbType()));
        }
    }

    //kombina to allow setting the unique index constraint on column repo_path_checksum
    private boolean isColumnNullable(Connection con, String tableName, String colName) throws SQLException {
        boolean columnNullable = false;
        DatabaseMetaData metadata = con.getMetaData();
        try (ResultSet rs = metadata.getColumns(null, null,
                normalizedName(tableName, metadata), normalizedName(colName, metadata))) {
            if (rs.next()) {
                columnNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
            }
        }
        return columnNullable;
    }

    private void makeChecksumColumnUnique(Connection con) throws SQLException {
        Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try (ResultSet rs = statement.executeQuery("SELECT node_id, repo_path_checksum FROM nodes WHERE repo_path_checksum IS NULL")) {
            int i = 0;
            while (rs.next()) {
                rs.updateString("repo_path_checksum", "a" + i);
                rs.updateRow();
                i++;
            }
        } finally {
            statement.close();
        }
    }
}

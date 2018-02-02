package org.artifactory.storage.db.upgrades.version;

import org.artifactory.storage.db.conversion.version.v202.V202ConversionPredicate;
import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.artifactory.storage.db.version.converter.ConditionalDBSqlConverter;
import org.jfrog.storage.util.DbStatementUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test DB version v202 (art version v531)
 * This conversion runs conditionally for upgrades from version < 4.4.1 up to 5.3.0 * that had their v108
 * (art version v441 -> v4141) conversion messed up.
 *
 * @author Dan Feldman
 */
@Test
public class V202UpgradeTest extends UpgradeBaseTest {

    //Conversion from <= 4.4.1 to 5.3.0 omitted the v441 conversion (i.e. v108 db version)
    //To simulate this all conversions are run here up to 5.3.0 without 4.4.1 and then 5.3.1 (db v202) is run
    public void testConversionFromV441ToV530() throws IOException, SQLException {
        rollBackTo300Version();
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            //now at db v100 run all conversions up to v201 omitting v108
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v310", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v311", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v410", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v420", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v440", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v500", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v500a", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v530", dbProperties.getDbType()));

            //v441 (db v108) should be missing
            assertFalse(DbTestUtils.isColumnExist(connection, "users", "credentials_expired"));
            //run conditional conversion v202
            ConditionalDBSqlConverter v202 = new ConditionalDBSqlConverter("v441", new V202ConversionPredicate());
            v202.convert(jdbcHelper, dbProperties.getDbType());
            //v108 should have been run
            assertTrue(DbTestUtils.isColumnExist(connection, "users", "credentials_expired"));
        }
    }

    //Tests that the conditional conversion will not run if not needed (upgrading to 5.3.1 from anything later then 4.4.1)
    public void testConversionFromV4142ToV530() throws IOException, SQLException {
        rollBackTo300Version();
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            //now at db v100 run all conversions up to v201 omitting v108
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v310", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v311", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v410", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v420", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v440", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v441", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v500", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v500a", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getDbSchemaUpgradeSql("v530", dbProperties.getDbType()));

            //v441 (db v108) should have run ok
            assertTrue(DbTestUtils.isColumnExist(connection, "users", "credentials_expired"));
            //run conditional conversion v202
            try {
                ConditionalDBSqlConverter v202 = new ConditionalDBSqlConverter("v441", new V202ConversionPredicate());
                v202.convert(jdbcHelper, dbProperties.getDbType());
            } catch (Exception e) {
                throw new RuntimeException("Conditional db V202 conversion should not have run: " + e.getMessage(), e);
            }
        }
    }
}

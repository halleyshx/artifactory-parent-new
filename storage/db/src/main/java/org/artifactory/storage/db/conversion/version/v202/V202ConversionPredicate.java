package org.artifactory.storage.db.conversion.version.v202;

import org.artifactory.storage.db.conversion.ConversionPredicate;
import org.artifactory.storage.db.util.JdbcHelper;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.function.BiPredicate;

/**
 * @author Dan Feldman
 */
public class V202ConversionPredicate implements ConversionPredicate {
    private static final Logger log = LoggerFactory.getLogger(V202ConversionPredicate.class);

    @Override
    public BiPredicate<JdbcHelper, DbType> condition() {
            return (jdbcHelper, dbType) -> {
                Connection conn = null;
                try {
                    conn = jdbcHelper.getDataSource().getConnection();
                    // run conversion if column credentials_expired is missing from schema
                    return !DbUtils.columnExists(conn.getMetaData(), "users", "credentials_expired");
                } catch (Exception e) {
                    log.error("Cannot run conversion 'v202' - Failed to retrieve schema metadata: ", e);
                } finally {
                    DbUtils.close(conn);
                }
                return false;
            };
    }
}

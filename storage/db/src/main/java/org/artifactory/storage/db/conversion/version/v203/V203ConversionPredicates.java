package org.artifactory.storage.db.conversion.version.v203;

import org.artifactory.storage.db.conversion.ConversionPredicate;
import org.artifactory.storage.db.util.JdbcHelper;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * All predicates for v5.5.0 conversions (sha2 and no-hazelcast)
 *
 * @author Dan Feldman
 */
public class V203ConversionPredicates {
    private static final Logger log = LoggerFactory.getLogger(V203ConversionPredicates.class);

    /**
     * binaries table sha256 conversion predicate
     */
    public static class V550ConversionPredicate implements ConversionPredicate {
        @Override
        public BiPredicate<JdbcHelper, DbType> condition() {
            return (jdbcHelper, dbType) ->
                    test(metadata -> {
                        try {
                            return !DbUtils.columnExists(metadata, "binaries", "sha256");
                        } catch (SQLException e) {
                            log.error("Cannot run conversion 'v550' - Failed to resolve schema metadata: ", e);
                        }
                        return false;
                    }, jdbcHelper, "v550");
        }
    }

    /**
     * nodes table sha256 and no-hazelcast conversion predicate
     */
    public static class V550aConversionPredicate implements ConversionPredicate {
        @Override
        public BiPredicate<JdbcHelper, DbType> condition() {
            return (jdbcHelper, dbType) ->
                    test(metadata -> {
                        try {
                            return !DbUtils.columnExists(metadata, "nodes", "sha256")
                                    && !DbUtils.columnExists(metadata, "nodes", "repo_path_checksum");
                        } catch (SQLException e) {
                            log.error("Cannot run conversion 'v550a' - Failed to resolve schema metadata: ", e);
                        }
                        return false;
                    }, jdbcHelper, "v550a");
        }
    }

    /**
     * nodes table sha256 index conversion predicate
     */
    public static class V550bConversionPredicate implements ConversionPredicate {
        @Override
        public BiPredicate<JdbcHelper, DbType> condition() {
            return (jdbcHelper, dbType) ->
                    test(metadata -> {
                        try {
                            return !DbUtils.indexExists(jdbcHelper, "nodes", "sha256", "nodes_sha256_idx", dbType);
                        } catch (SQLException e) {
                            log.error("Cannot run conversion 'v550b' - Failed to resolve schema metadata: ", e);
                        }
                        return false;
                    }, jdbcHelper, "v550b");
        }
    }

    /**
     * distributed_locks table conversion predicate
     */
    public static class V550cConversionPredicate implements ConversionPredicate {
        @Override
        public BiPredicate<JdbcHelper, DbType> condition() {
            return (jdbcHelper, dbType) ->
                    test(metadata -> {
                        try {
                            return !DbUtils.tableExists(metadata, "distributed_locks");
                        } catch (SQLException e) {
                            log.error("Cannot run conversion 'v550c' - Failed to resolve schema metadata: ", e);
                        }
                        return false;
                    }, jdbcHelper, "v550c");
        }
    }

    private static boolean test(Predicate<DatabaseMetaData> conversionPredicate, JdbcHelper jdbcHelper, String conversionName) {
        Connection conn = null;
        try {
            conn = jdbcHelper.getDataSource().getConnection();
            return conversionPredicate.test(conn.getMetaData());
        } catch (Exception e) {
            log.error("Cannot run conversion '" + conversionName + "' - Failed to retrieve schema metadata: ", e);
        } finally {
            DbUtils.close(conn);
        }
        log.debug("sha256 column in nodes table already exists, skipping '" + conversionName + "' conversion.");
        return false;
    }
}

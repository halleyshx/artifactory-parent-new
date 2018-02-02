package org.artifactory.storage.db.version.converter;

import org.jfrog.common.ResourceUtils;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbStatementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Dan Feldman
 */
public class DbSqlConverterUtil {
    private static final Logger log = LoggerFactory.getLogger(DbSqlConverterUtil.class);

    public static void convert(Connection conn, DbType dbType, String fromVersion) {
        try {
            // Build resource file name.
            String dbTypeName = dbType.toString();
            String resourcePath = "/conversion/" + dbTypeName + "/" + dbTypeName + "_" + fromVersion + ".sql";
            InputStream resource = ResourceUtils.getResource(resourcePath);
            if (resource == null) {
                throw new IOException("Database DDL resource not found at: '" + resourcePath + "'");
            }
            // Execute update
            log.info("Starting schema conversion: {}", resourcePath);
            DbStatementUtils.executeSqlStream(conn, resource);
            log.info("Finished schema conversion: {}", resourcePath);
        } catch (SQLException | IOException e) {
            String msg = "Could not convert DB using " + fromVersion + " converter";
            log.error(msg + " due to " + e.getMessage(), e);
            throw new RuntimeException(msg, e);
        }
    }
}

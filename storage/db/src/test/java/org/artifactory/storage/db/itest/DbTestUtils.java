/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.storage.db.itest;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.jfrog.common.ResourceUtils;
import org.jfrog.security.util.Pair;
import org.jfrog.storage.DbType;
import org.jfrog.storage.JdbcHelper;
import org.jfrog.storage.util.DbStatementUtils;
import org.jfrog.storage.util.DbUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Collections;
import java.util.List;

import static org.jfrog.storage.util.DbUtils.*;
import static org.testng.Assert.fail;

/**
 * A utility class for integration tests to clean and setup the database
 *
 * @author Yossi Shaul
 */
public class DbTestUtils {
    // BE CAREFUL do not have logger here. Tests not initialized correctly

    /**
     * A list of all the tables in the database
     */
    private static final List<String> tables = Collections.unmodifiableList(Lists.newArrayList(
            "db_properties", "artifactory_servers",
            "stats_remote", "stats", "watches", "node_props", "node_meta_infos", "nodes",
            "indexed_archives_entries", "archive_names", "archive_paths", "indexed_archives",
            "binary_blobs", "binaries",
            "aces", "acls", "users_groups", "groups", "user_props", "users",
            "permission_target_repos", "permission_targets",
            "configs", "tasks",
            "module_props", "build_props", "build_jsons", "build_promotions",
            "build_dependencies", "build_artifacts", "build_modules", "builds",
            "unique_ids", "distributed_locks", "master_key_status"
    ));

    //Testing for table existence is not enough, some schema conversions change columns as well.
    //Ordered by most probable table to change
    private static final List<Pair<String,List<String>>> schema = Collections.unmodifiableList(Lists.newArrayList(
            new Pair<String, List<String>>("binaries", Lists.newArrayList("sha1", "md5", "bin_length", "sha256")),
            new Pair<String, List<String>>("nodes", Lists.newArrayList("node_id", "node_type", "repo", "node_path", "node_name", "depth",
                    "created", "created_by", "modified", "modified_by", "updated", "bin_length", "sha1_actual", "sha1_original", "md5_actual",
                    "md5_original", "sha256", "repo_path_checksum")),
            new Pair<String, List<String>>("binary_blobs", Lists.newArrayList("sha1", "data")),
            new Pair<String, List<String>>("aces", Lists.newArrayList("ace_id", "acl_id", "mask", "user_id", "group_id")),
            new Pair<String, List<String>>("acls", Lists.newArrayList("acl_id", "perm_target_id", "modified", "modified_by")),
            new Pair<String, List<String>>("users_groups", Lists.newArrayList("user_id", "group_id", "realm")),
            new Pair<String, List<String>>("groups", Lists.newArrayList("group_id", "group_name", "description", "default_new_user", "realm",
                    "realm_attributes", "admin_privileges")),
            new Pair<String, List<String>>("users", Lists.newArrayList("user_id", "username", "password", "salt", "email", "gen_password_key",
                    "admin", "enabled", "updatable_profile", "realm", "private_key", "public_key", "last_login_time", "last_login_ip",
                    "last_access_time", "last_access_ip", "bintray_auth", "locked", "credentials_expired")),
            new Pair<String, List<String>>("build_dependencies", Lists.newArrayList("dependency_id", "module_id", "dependency_name_id",
                    "dependency_scopes", "dependency_type", "sha1", "md5", "sha256")),
            new Pair<String, List<String>>("build_artifacts", Lists.newArrayList("artifact_id", "module_id", "artifact_name",
                    "artifact_type", "sha1", "md5", "sha256")),
            new Pair<String, List<String>>("build_promotions", Lists.newArrayList("build_id", "created", "created_by", "status", "repo",
                    "promotion_comment", "ci_user")),
            new Pair<String, List<String>>("builds", Lists.newArrayList("build_id", "build_name", "build_number", "build_date",
                    "ci_url", "created", "created_by", "modified", "modified_by")),
            new Pair<String, List<String>>("build_modules", Lists.newArrayList("module_id", "build_id", "module_name_id")),
            new Pair<String, List<String>>("artifactory_servers", Lists.newArrayList("server_id", "start_time", "context_url", "membership_port", "server_state",
                    "server_role", "last_heartbeat", "artifactory_version", "artifactory_revision", "artifactory_release", "artifactory_running_mode", "license_hash")),
            new Pair<String, List<String>>("db_properties", Lists.newArrayList("installation_date", "artifactory_version", "artifactory_revision", "artifactory_release")),
            new Pair<String, List<String>>("stats_remote", Lists.newArrayList("node_id", "origin", "download_count", "last_downloaded", "last_downloaded_by", "path")),
            new Pair<String, List<String>>("stats", Lists.newArrayList("node_id", "download_count", "last_downloaded", "last_downloaded_by")),
            new Pair<String, List<String>>("watches", Lists.newArrayList("watch_id", "node_id", "username", "since")),
            new Pair<String, List<String>>("node_props", Lists.newArrayList("prop_id", "node_id", "prop_key", "prop_value")),
            new Pair<String, List<String>>("node_meta_infos", Lists.newArrayList("node_id", "props_modified", "props_modified_by")),
            new Pair<String, List<String>>("indexed_archives_entries", Lists.newArrayList("indexed_archives_id", "entry_path_id", "entry_name_id")),
            new Pair<String, List<String>>("archive_names", Lists.newArrayList("name_id", "entry_name")),
            new Pair<String, List<String>>("archive_paths", Lists.newArrayList("path_id", "entry_path")),
            new Pair<String, List<String>>("indexed_archives", Lists.newArrayList("archive_sha1", "indexed_archives_id")),
            new Pair<String, List<String>>("user_props", Lists.newArrayList("user_id", "prop_key", "prop_value")),
            new Pair<String, List<String>>("permission_target_repos", Lists.newArrayList("perm_target_id", "repo_key")),
            new Pair<String, List<String>>("permission_targets", Lists.newArrayList("perm_target_id", "perm_target_name", "includes", "excludes")),
            new Pair<String, List<String>>("configs", Lists.newArrayList("config_name", "last_modified", "data")),
            new Pair<String, List<String>>("tasks", Lists.newArrayList("task_type", "task_context", "created")),
            new Pair<String, List<String>>("module_props", Lists.newArrayList("prop_id", "module_id", "prop_key", "prop_value")),
            new Pair<String, List<String>>("build_props", Lists.newArrayList("prop_id", "build_id", "prop_key", "prop_value")),
            new Pair<String, List<String>>("build_jsons", Lists.newArrayList("build_id", "build_info_json")),
            new Pair<String, List<String>>("unique_ids", Lists.newArrayList("index_type", "current_id"))
            //TODO [by shayb]: do we need the master_key_status here?
    ));

    /**
     * A list of all the access server tables inside the artifactory database (since access server is embedded)
     */
    private static final List<String> accessServerTables = Collections.unmodifiableList(Lists.newArrayList(
            "access_tokens", "access_users", "access_servers"
    ));

    public static void refreshOrRecreateSchema(Logger log, Connection conn, DbType dbType) throws IOException, SQLException {
        // to improve test speed, re-create the schema only if there's a missing table
        boolean recreateSchema = isSchemaIncomplete(conn);
        if (recreateSchema) {
            log.info("Recreating test database schema for database: {}", dbType);
            dropAllExistingTables(conn);
            createSchema(conn, dbType);
        } else {
            log.info("Deleting database tables data from database: {}", dbType);
            deleteFromAllTables(conn);
            deleteFromAccessServerTables(conn);
        }
    }

    public static void deleteSchema(String schema, Connection con, DbType dbtype) {
        try (Statement statement = con.createStatement()) {
            statement.execute("DROP SCHEMA "+schema+" CASCADE;");
        } catch (SQLException e) {
            //System.out.println("Failed to delete from access server table '" + table + "', ignoring. (" + e + ")");
        }
    }

    public static void dropAllExistingTables(Connection con) throws SQLException {
        for (String table : tables) {
            if (tableExists(con, table)) {
                try (Statement statement = con.createStatement()) {
                    statement.execute("DROP TABLE " + table);
                }
            }
        }
    }

    private static void deleteFromAllTables(Connection con) throws SQLException {
        for (String table : tables) {
            try (Statement statement = con.createStatement()) {
                statement.execute("DELETE FROM " + table);
            }
        }
    }

    private static void deleteFromAccessServerTables(Connection con) {
        for (String table : accessServerTables) {
            try (Statement statement = con.createStatement()) {
                statement.execute("DELETE FROM " + table);
            } catch (SQLException e) {
                //System.out.println("Failed to delete from access server table '" + table + "', ignoring. (" + e + ")");
            }
        }
    }

    public static boolean tableExists(Connection con, String tableName) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        tableName = normalizedName(tableName, metaData);
        String schemaName = whatIsMySchema(con);
        try (ResultSet rs = metaData.getTables(null, schemaName, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public static boolean isTableMissing(Connection con) throws SQLException {
        for (String table : tables) {
            if (!tableExists(con, table)) {
                return true;
            }
        }
        return false;
    }

    //TODO [by dan]: is this somehow related to test env only? moved it here from production code, need to ask bagants about it
    public static boolean indexNotExists(JdbcHelper jdbcHelper, String tableName, String columnName, String indexName,
            DbType dbType) throws SQLException {
        if (dbType.equals(DbType.DERBY)) {
            return indexNotExistsDerby(jdbcHelper, indexName);
        }
        return !indexExists(jdbcHelper, tableName, columnName, indexName, dbType);
    }

    //TODO [by dan]: is this somehow related to test env only? moved it here from production code, need to ask bagants about it
    private static boolean indexNotExistsDerby(JdbcHelper jdbcHelper, String indexName) throws SQLException {
        try (ResultSet resultSet = jdbcHelper.executeSelect("SELECT * FROM SYS.SYSCONGLOMERATES")) {
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("CONGLOMERATENAME"))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tests the db schema for missing columns, enough that once is missing to return true.
     */
    private static boolean isSchemaIncomplete(Connection con) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        for (Pair<String, List<String>> tableColumns : schema) {
            String table = tableColumns.getFirst();
            for (String column : tableColumns.getSecond()) {
                if (!DbUtils.columnExists(metaData, table, column)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isColumnExist(Connection con, String table, String column) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        String tableName = normalizedName(table, metaData);
        String columnName = normalizedName(column, metaData);
        String schemaName = whatIsMySchema(con);
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, columnName)) {
            return rs.next();
        }
    }

    public static int getColumnSize(Connection con, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        tableName = normalizedName(tableName, metaData);
        columnName = normalizedName(columnName, metaData);

        try (Statement statement = con.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * from " + tableName)) {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                if (resultSetMetaData.getColumnName(i).equals(columnName)) {
                    return resultSetMetaData.getColumnDisplaySize(i);
                }
            }
        }

        return -1;
    }

    private static void createSchema(Connection con, DbType dbType) throws SQLException, IOException {
        // read ddl from file and execute
        DbStatementUtils.executeSqlStream(con, getDbSchemaSql(dbType));
    }

    private static InputStream getDbSchemaSql(DbType dbType) {
        String dbConfigDir = dbType.toString();
        return ResourceUtils.getResource("/" + dbConfigDir + "/" + dbConfigDir + ".sql");
    }

    public static void forceShutdownDerby(Logger log, String derbyDbHome) {
        if (derbyDbHome == null) {
            // Nothing to do
            return;
        }
        // Shutdown Jetty if needed
        File dbHome = new File(derbyDbHome);
        if (dbHome.exists()) {
            try {
                log.info("Shutting down Derby DB '{}'", dbHome.getAbsolutePath());
                // Forcing Derby shutdown
                DriverManager.getConnection("jdbc:derby:" + dbHome.getAbsolutePath() + ";shutdown=true");
            } catch (SQLException e) {
                // Good one should be ERROR 08006 for shutdown
                boolean containsGoodError = e instanceof SQLNonTransientConnectionException &&
                        e.getMessage().contains("shutdown");
                if (!containsGoodError) {
                    String msg = "Exception should have words 'Database shutdown' but we got: '" + e.getMessage() + "'";
                    log.error(msg, e);
                    fail(msg);
                } else {
                    log.info(e.getMessage());
                }
            }
            try {
                // Here we managed to shutdown Derby correctly. Let's remove the folder
                FileUtils.deleteDirectory(dbHome);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

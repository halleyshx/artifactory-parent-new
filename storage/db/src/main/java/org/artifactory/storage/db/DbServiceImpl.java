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

package org.artifactory.storage.db;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.config.db.ArtifactoryDbProperties;
import org.artifactory.common.storage.db.properties.DbVersionInfo;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.mbean.MBeanRegistrationService;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.db.fs.dao.NodesDao;
import org.artifactory.storage.db.mbean.ManagedDataSource;
import org.artifactory.storage.db.mbean.NewDbInstallation;
import org.artifactory.storage.db.properties.service.ArtifactoryDbPropertiesService;
import org.artifactory.storage.db.spring.ArtifactoryDataSource;
import org.artifactory.storage.db.util.IdGenerator;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.version.ArtifactoryDBVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.common.ResourceUtils;
import org.jfrog.storage.DbType;
import org.jfrog.storage.priviledges.DBPrivilegesVerifierFactory;
import org.jfrog.storage.util.DbStatementUtils;
import org.jfrog.storage.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import static org.jfrog.storage.util.DbUtils.normalizedName;

/**
 * @author Yossi Shaul
 */
@Repository
@Reloadable(beanClass = DbService.class)
public class DbServiceImpl implements InternalDbService {
    private static final Logger log = LoggerFactory.getLogger(DbServiceImpl.class);

    private static final double MYSQL_MIN_VERSION = 5.5;

    //I am the god of legacy code, gaze upon me and despair :(
    private boolean sha256Ready = false; // This means migration ran ok.
    private boolean uniqueRepoPathChecksumReady = false;

    @Autowired
    private JdbcHelper jdbcHelper;

    @Autowired
    @Qualifier("dbProperties")
    private ArtifactoryDbProperties dbProperties;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private ArtifactoryDbPropertiesService dbPropertiesService;

    @Autowired
    private ApplicationEventPublisher publisher;

    public static boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        return DbUtils.tableExists(metaData, tableName);
    }

    @Override
    @PostConstruct
    public void initDb() throws Exception {
        printConnectionInfo();

        // check if db tables exist and initialize if not
        if (!isSchemaExist()) {
            try (Connection con = jdbcHelper.getDataSource().getConnection()) {
                // if using mySQL, check version compatibility
                if (dbProperties.getDbType() == DbType.MYSQL) {
                    checkMySqlMinVersion();
                }
                // read ddl from file and execute
                log.info("***Creating database schema***");
                DbStatementUtils.executeSqlStream(con, getDbSchemaSql());
                broadcastNewDbInstallation();
                updateDbProperties();
                //new schema means nothing to convert.
                sha256Ready = true;
                uniqueRepoPathChecksumReady = true;
            }
        } else {
            if (dbPropertiesService.getDbVersionInfo() == null) {
                broadcastNewDbInstallation();
            }
        }

        // initialize id generator
        initializeIdGenerator();
    }

    private void broadcastNewDbInstallation() {
        publisher.publishEvent(new NewDbInstallation(getClass().getSimpleName()));
    }

    private void updateDbProperties() {
        // Update DBProperties
        long installTime = System.currentTimeMillis();
        CompoundVersionDetails versionDetails = ArtifactoryHome.get().getRunningArtifactoryVersion();
        String versionStr = versionDetails.getVersion().getValue();
        long timestamp = versionDetails.getTimestamp();
        int revisionInt = versionDetails.getRevisionInt();
        dbPropertiesService.updateDbVersionInfo(new DbVersionInfo(installTime, versionStr, revisionInt, timestamp));
    }

    @Override
    public void init() {
        registerDataSourceMBean();
        //TODO [by dan]: possibly not the best place for this...
        verifySha256State();
        verifyUniqueRepoPathChecksumState();
    }

    @Override
    public DbType getDatabaseType() {
        return dbProperties.getDbType();
    }

    @Override
    public long nextId() {
        return idGenerator.nextId();
    }

    @Override
    public void compressDerbyDb(BasicStatusHolder statusHolder) {
        DerbyUtils.compress(statusHolder);
    }

    @Override
    public <T> T invokeInTransaction(String transactionName, Callable<T> execute) {
        if (StringUtils.isNotBlank(transactionName)) {
            TransactionSynchronizationManager.setCurrentTransactionName(transactionName);
        }
        try {
            return execute.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    //used via reflection by DbBaseTest
    private void initializeIdGenerator() throws SQLException {
        idGenerator.initializeIdGenerator();
    }

    private InputStream getDbSchemaSql() throws IOException {
        String dbTypeName = dbProperties.getDbType().toString();
        String resourcePath = "/" + dbTypeName + "/" + dbTypeName + ".sql";
        InputStream resource = ResourceUtils.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("Database DDL resource not found at: '" + resourcePath + "'");
        }
        return resource;
    }

    private boolean isSchemaExist() throws SQLException {
        log.debug("Checking for database schema existence");
        Connection conn = null;
        try {
            conn = jdbcHelper.getDataSource().getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            return tableExists(metaData, NodesDao.TABLE_NAME);
        } finally {
            DbUtils.close(conn);
        }
    }

    private void printConnectionInfo() throws SQLException {
        Connection connection = null;
        try {
            connection = jdbcHelper.getDataSource().getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            log.info("Database: {} {}. Driver: {} {} Pool: {}", meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion(),
                    meta.getDriverName(), meta.getDriverVersion(), dbProperties.getDbType());
            log.info("Connection URL: {}", meta.getURL());
        } catch (SQLException e) {
            log.warn("Can not retrieve database and driver name / version", e);
        } finally {
            DbUtils.close(connection);
        }
    }

    private void registerDataSourceMBean() {
        DataSource dataSource = jdbcHelper.getDataSource();
        if (dataSource instanceof ArtifactoryDataSource) {
            ArtifactoryDataSource artifactoryDS = (ArtifactoryDataSource) dataSource;
            MBeanRegistrationService mbeansService = ContextHelper.get().beanForType(MBeanRegistrationService.class);
            mbeansService.register(new ManagedDataSource(artifactoryDS, jdbcHelper), "Storage", "Data Source");
            artifactoryDS.registerMBeans(mbeansService);
        }
    }

    private boolean checkMySqlMinVersion() {
        log.debug("Checking MySQL version compatibility");
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT VERSION();");
            if (rs.next()) {
                String versionString = rs.getString(1);
                int i = StringUtils.ordinalIndexOf(versionString, ".", 2);
                if (i == -1) {
                    i = versionString.length();
                }
                Double mysqlVersion = Double.valueOf(versionString.substring(0, i));
                if (mysqlVersion >= MYSQL_MIN_VERSION) {
                    return true;
                } else {
                    log.error("Unsupported MySQL version found [" + versionString + "]. " +
                            "Minimum version required is " + MYSQL_MIN_VERSION + ". " +
                            "Please follow the requirements on the wiki page.");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Could not determine MySQL version due to an exception", e);
        } finally {
            DbUtils.close(rs);
        }
        log.error("Could not determine MySQL version. Minimum version should be " + MYSQL_MIN_VERSION + " and above.");
        return false;
    }

    public void enforceDBPrivileges() {
        File marker = ArtifactoryHome.get().getSkipVerifyPrivilegesMarkerFile();
        if (!marker.exists()) {
            try (Connection con = jdbcHelper.getDataSource().getConnection()) {
                if (!DBPrivilegesVerifierFactory.createDBPrivilegesVerifier(dbProperties.getDbType())
                        .isSufficientPrivileges(con, "artifactory")) {
                    log.error("Insufficient DB privileges found!. Not starting migration.");
                    throw new RuntimeException("Insufficient DB privileges found!");
                }
            } catch (SQLException e) {
                log.error("Error while verifying DB privileges. Not starting migration.");
                throw new RuntimeException("Error while verifying DB privileges", e);
            }
        }
        marker.delete();
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        enforceDBPrivileges();
        ArtifactoryDBVersion.convert(source.getVersion(), jdbcHelper, dbProperties.getDbType());
        updateDbProperties();
        verifySha256State();
        verifyUniqueRepoPathChecksumState();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        InternalDbService txme = ContextHelper.get().beanForType(InternalDbService.class);
        txme.verifyMigrations();
    }

    @Override
    public void destroy() {
        jdbcHelper.destroy();
    }

    /**
     * TO BE USED ONLY BY THE SHA256 MIGRATION JOB
     * Tests the db metadata for the sha256 column's (in binaries table) nullable state,
     * and sets the state's flag {@link #sha256Ready} accordingly
     * @return {@link #sha256Ready}
     */
    @Override
    public boolean verifySha256State() {
        if (sha256Ready) {
            //Already verified to be ok, no need to do it again
            return true;
        }
        Connection conn = null;
        try {
            conn = jdbcHelper.getDataSource().getConnection();
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet rs = metadata.getColumns(null, null,
                    normalizedName("binaries", metadata), normalizedName("sha256", metadata))) {
                if (rs.next()) {
                    sha256Ready = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                } else {
                    log.warn("Can't determine state of sha256 column in binaries table, column not found in db metadata.");
                }
            }
        } catch (Exception e) {
            log.warn("Can't determine state of sha256 column in binaries table: {}", e.getMessage());
            log.debug("", e);
        } finally {
            DbUtils.close(conn);
        }
        log.debug("Determined SHA256 readiness state to be: {}", sha256Ready);
        return sha256Ready;
    }

    /**
     * TO BE USED ONLY BY THE REPO_PATH_CHECKSUM MIGRATION JOB
     * Tests the db metadata for the repoPathChecksum column's (in nodes table) unique index state,
     * and sets the state's flag {@link #uniqueRepoPathChecksumReady} accordingly
     * @return {@link #uniqueRepoPathChecksumReady}
     */
    @Override
    public boolean verifyUniqueRepoPathChecksumState() {
        if (uniqueRepoPathChecksumReady) {
            //Already verified to be ok, no need to do it again
            return true;
        }
        try {
            uniqueRepoPathChecksumReady = DbUtils.indexExists(jdbcHelper, "nodes",
                    "repo_path_checksum", "nodes_repo_path_checksum", dbProperties.getDbType());
        } catch (Exception e) {
            log.warn("Can't determine the uniqueness of 'repo_path_checksum' column column in nodes table: {}", e.getMessage());
            log.debug("", e);
        }
        if (uniqueRepoPathChecksumReady) {
            log.debug("Artifactory is running with full repo path checksum support.");
        } else {
            log.debug("Full repo path checksum support is not active yet.");
        }
        log.debug("Determined repoPathChecksum readiness state to be: {}", uniqueRepoPathChecksumReady);
        return uniqueRepoPathChecksumReady;
    }

    @Override
    public boolean isSha256Ready() {
        return sha256Ready;
    }

    @Override
    public boolean isUniqueRepoPathChecksumReady() {
        return uniqueRepoPathChecksumReady;
    }

    @Override
    public void verifyMigrations() {
        verifySha256State();
        verifyUniqueRepoPathChecksumState();
    }
}

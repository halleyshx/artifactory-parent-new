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

package org.artifactory.storage.db.upgrades.common;

import ch.qos.logback.classic.util.ContextInitializer;
import org.artifactory.common.config.db.ArtifactoryDbProperties;
import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.spring.ArtifactoryDataSource;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.version.ArtifactoryDBVersion;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.version.ArtifactoryVersion;
import org.jfrog.common.ResourceUtils;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbStatementUtils;
import org.jfrog.storage.util.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.artifactory.storage.db.version.ArtifactoryDBVersion.convert;
import static org.artifactory.storage.db.version.ArtifactoryDBVersion.v100;
import static org.testng.Assert.assertEquals;

/**
 * Date: 8/5/13 9:58 AM
 *
 * @author freds
 */
@Test(groups = "dbtest-upgrade")
@ContextConfiguration(locations = {"classpath:spring/db-upgrade-test-context.xml"})
public abstract class UpgradeBaseTest extends AbstractTestNGSpringContextTests {
    static {
        // use the itest logback config
        URL url = UpgradeBaseTest.class.getClassLoader().getResource("logback-dbtest.xml");
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, url.getPath());
    }

    @Autowired
    protected JdbcHelper jdbcHelper;

    @Autowired
    @Qualifier("dbProperties")
    protected ArtifactoryDbProperties dbProperties;

    private ArtifactoryHomeBoundTest artifactoryHomeBoundTest;

    protected static InputStream getPreviousDbSchemaSql(String previousVersion, DbType dbType) {
        String dbConfigDir = dbType.toString();
        return ResourceUtils.getResource(
                "/upgrades/" + previousVersion + "/ddl/" + dbConfigDir + "/" + dbConfigDir + ".sql");
    }

    protected static InputStream getPreviousImportSql(String previousVersion, String dataFileName) {
        return ResourceUtils.getResource(
                "/upgrades/" + previousVersion + "/data/" + dataFileName + ".sql");
    }

    protected static InputStream getDbSchemaUpgradeSql(String forVersion, DbType dbType) {
        String dbConfigDir = dbType.toString();
        return ResourceUtils.getResource(
                "/conversion/" + dbConfigDir + "/" + dbConfigDir + "_" + forVersion + ".sql");
    }

    @BeforeClass
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        artifactoryHomeBoundTest = createArtifactoryHomeTest();
        artifactoryHomeBoundTest.bindArtifactoryHome();
        super.springTestContextPrepareTestInstance();

        rollBackTo300Version();
        convert(getFromVersion(v100), jdbcHelper, dbProperties.getDbType());
    }

        private ArtifactoryHomeBoundTest createArtifactoryHomeTest() {
        return new ArtifactoryHomeBoundTest();
    }

    @AfterMethod
    protected void verifyDbResourcesReleased() {
        // make sure there are no active connections
        ArtifactoryDataSource ds = (ArtifactoryDataSource) jdbcHelper.getDataSource();
        assertEquals(ds.getActiveConnectionsCount(), 0, "Found " + ds.getActiveConnectionsCount() +
                " active connections after test ended");
        artifactoryHomeBoundTest.unbindArtifactoryHome();
    }

    @BeforeMethod
    public void bindArtifactoryHome() {
        artifactoryHomeBoundTest.bindArtifactoryHome();
    }

    @AfterMethod
    public void unbindArtifactoryHome() {
        artifactoryHomeBoundTest.unbindArtifactoryHome();
    }

    protected void rollBackTo300Version() throws SQLException, IOException {
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            // Making DB looks like v300 with data
            DbTestUtils.dropAllExistingTables(connection);
            DbStatementUtils.executeSqlStream(connection, getPreviousDbSchemaSql("v300", dbProperties.getDbType()));
            DbStatementUtils.executeSqlStream(connection, getPreviousImportSql("v300", "user-group"));
            DbStatementUtils.executeSqlStream(connection, getPreviousImportSql("v300", "acls"));
            DbStatementUtils.executeSqlStream(connection, getPreviousImportSql("v300", "builds"));
            DbStatementUtils.executeSqlStream(connection, getPreviousImportSql("v300", "nodes"));
        }
    }

    protected void resetToVersion(ArtifactoryDBVersion resetTo) throws IOException, SQLException {
        rollBackTo300Version();
        Stream.of(ArtifactoryDBVersion.values())
                .filter(version -> versionToLong(resetTo) >= versionToLong(version))
                .flatMap((version) -> Stream.of(version.getConverters()))
                .forEachOrdered(converter -> converter.convert(jdbcHelper, dbProperties.getDbType()));
    }

    private long versionToLong(ArtifactoryDBVersion version) {
        return Long.parseLong(version.name().substring(1));
    }

    protected void importSql(String resourcePath) {
        InputStream resource = ResourceUtils.getResource(resourcePath);
        Connection con = null;
        try {
            con = jdbcHelper.getDataSource().getConnection();
            DbStatementUtils.executeSqlStream(con, resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.close(con);
        }
    }

    private ArtifactoryVersion getFromVersion(ArtifactoryDBVersion version) {
        return version.getComparator().getFrom();
    }
}

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

package org.artifactory.storage.db.build.itest.dao;

import org.artifactory.api.build.BuildProps;
import org.artifactory.api.build.diff.BuildParams;
import org.artifactory.storage.db.build.entity.BuildEntity;
import org.artifactory.storage.db.build.entity.BuildEntityRecord;
import org.artifactory.storage.db.build.entity.BuildPromotionStatus;
import org.artifactory.storage.db.build.entity.BuildProperty;
import org.artifactory.storage.db.build.service.BuildStoreServiceImpl;
import org.jfrog.storage.util.DbUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import static org.testng.Assert.assertEquals;

/**
 * Tested using : artifactory.5.x/artifactory-oss/distribution/standalone/src/test/etc/db.properties
 * with local postgres.
 * type=postgresql
 * driver=org.postgresql.Driver
 * url=jdbc:postgresql://localhost:5432/artifactory
 * username=artifactory
 * password=password
 */
@Test
public class BuildsLargeDaoChooseBestQueryTest extends BuildsDaoBaseTest {

    private BuildEntity build;

    @BeforeClass
    public void setup() throws UnsupportedEncodingException, SQLException {
        importSql("/sql/builds.sql");
        Instant start = Instant.now();

        build = createBuildLots(15, 100, lots, "15");
        assertEquals(createBuild(build, " " + build.getBuildName()), lots + 2);
        logger.info("duplicate " + lots);
        jdbcHelper.executeUpdate(
                " insert into build_props select prop_id+10000,build_id,prop_key,prop_value from build_props");
        numOfRows();

        /**
         * Blocks are false since we do want quick test. In dev we can use the big data and see behaviour.
         * Current size does not resembel real world since we should have lot's of builds so we we stream only several thousands .
         */
        if (false) { // big
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+20000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+40000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+80000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+160000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+320000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+640000,build_id,prop_key,prop_value from build_props");
            numOfRows();
            jdbcHelper.executeUpdate(
                    " insert into build_props select prop_id+1280000,build_id,prop_key,prop_value from build_props");
            numOfRows();

            BuildEntity build2 = createBuildLots(16, 80000000, lots, "other build");
            assertEquals(createBuild(build2, "jsonnn" + build2.getBuildName()), lots + 2);
            long buildId = build.getBuildId();
            BuildEntityRecord record = buildsDao.getBuildEntityRecord(buildId);

            if (true) { //bigger
                jdbcHelper.executeUpdate(
                        " insert into build_props select prop_id+2560000,build_id,prop_key,prop_value from build_props");
                numOfRows();
            }
        }
        //working with 5M rows having 8910 distinct values.

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        logger.info(String.format("setup took %s ", duration));

    }

    private long numOfRows() throws SQLException {
        try (ResultSet rs = jdbcHelper.executeSelect("select count(*) from build_props ")) {
            if (rs.next()) {
                logger.info(rs.getMetaData());
                long aLong = rs.getLong(1);
                logger.info("rows:" + aLong);
                return aLong;
            }
            return 0;

        }
    }

    protected BuildEntity createBuildLots(long id, int offset, int lots, String buildname) {
        long now = System.currentTimeMillis();
        BuildEntityRecord build = new BuildEntityRecord(id, buildname, "" + id, now - 20000L, null, now, "this-is-me",
                id, null);
        HashSet<BuildProperty> props = new HashSet<BuildProperty>();
        long buildId = build.getBuildId();
        for (int i = 0; i < lots; i++) {
            String key = "buildInfo.env.";
            String value = "" + i;
            if (i % 10 == 0) {
                key = key + "is % 10 ";
            } else if (i % 10 == 1) {
                key = "1  % 10 ";
            } else if (i % 10 == 2) {
                key = "2  % 10 " + i / 10;
            } else {
                key = "" + i;
                value = "" + i;
            }
            props.add(new BuildProperty(1 + offset + i, buildId, key, value));
        }
        return new BuildEntity(build,
                props,
                new TreeSet<BuildPromotionStatus>());
    }


    final private int lots = 10000 - 100;


    public static final String BUILD_SYSTEM_PROPS_BY_BUILD_ID = "\n" +
            "SELECT prop_key, prop_value from  build_props\n" +
            " WHERE build_id  = ? AND prop_key NOT LIKE 'buildInfo.env.%'";

    public static final String BUILD_SYSTEM_PROPS_BY_BUILD_ID_DISTINCT_REFERENCE = "\n" +
            "SELECT distinct prop_key, prop_value FROM  build_props\n" +
            " WHERE build_id  = ? AND prop_key NOT LIKE 'buildInfo.env.%'";


    @Test
    public void testGetDistinctBuildPropsResultSetOnQuery() throws Exception {
        //
        BuildParams buildParams = new BuildParams(null, build.getBuildNumber(), null,
                null, "" + build.getBuildDate(), null);
        //Long buildId = 16L;
        Long buildId = buildsDao.getBuildId(buildParams);
        int idx = 0;
        int distinct_rows = 8910;
        Duration durationReferene = null;
        logger.info("measure select distinct for comparision ");
        {
            ResultSet rs = null;
            try {
                String q = BUILD_SYSTEM_PROPS_BY_BUILD_ID_DISTINCT_REFERENCE;
                Instant start = Instant.now();
                int cnt = 0;
                rs = jdbcHelper.executeSelect(q, new Object[]{buildId});
                while (rs.next()) {
                    cnt++;
                }
                Instant end = Instant.now();
                Duration duration = Duration.between(start, end);
                durationReferene = duration;
                logger.info(String.format("REFERENCE Q%d took %s -- %S #cnt %d", idx++,
                        duration, q, cnt));
                assertEquals(cnt, distinct_rows);
            } finally {
                DbUtils.close(rs);
            }
        }


        {
            String q = BUILD_SYSTEM_PROPS_BY_BUILD_ID;
            Instant start = Instant.now();

            List<BuildProps> lst = buildsDao.getBuildPropsList(q, buildId);
            int cnt = lst.size();
            Instant timeCollected = Instant.now();
            logger.info("read #cnt " + cnt + "  took: " + Duration.between(start, timeCollected));

            cnt = BuildStoreServiceImpl.distinctBuildProps(lst).size();
            logger.info("distinct - read #cnt " + cnt);
            assertEquals(cnt, distinct_rows);
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            logger.info(String.format("getDistinctBuildPropsResultSetOnQuery Q%d took %s -- %S #cnt %d", idx++,
                    duration, q, cnt));
            //assertTrue(duration.toMillis() * 3 < durationReferene.toMillis());

        }
    }


    @AfterClass
    public void fullDelete() throws SQLException {
        assertEquals(buildArtifactsDao.deleteAllBuildArtifacts(), 6);
        assertEquals(buildDependenciesDao.deleteAllBuildDependencies(), 5);
        assertEquals(buildModulesDao.deleteAllBuildModules(), 4);
        buildsDao.deleteAllBuilds();
    }

}

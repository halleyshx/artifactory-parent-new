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

package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.result.AqlEagerResult;
import org.testng.annotations.Test;

/**
 * @author Gidi Shabat
 */
public class AqlStatsOrientedSearchesTest extends AqlAbstractServiceTest {

    ///*Statistics search*/
    @Test
    public void findStatistics() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find({\"item.repo\" :\"repo1\"},{\"downloads\":{\"$gt\":0}})");
        // Returns 2 although there are 3 matching rows because distinct merges two identical rows.
        // Currently we don't see a valid use case where it matters.
        assertSize(queryResult, 2);
        assertStatistics(queryResult, 15, "yossis");
    }

    @Test
    public void findStatisticsIncludeAll() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find().include(\"*\")");
        assertSize(queryResult, 5);
        assertStatistics(queryResult, 15, "yossis");
        assertStatistics(queryResult, 9, "yossis");
        assertStatisticsRemote(queryResult, 17, "dodo", "remote-host1", "path/a");
        assertStatisticsRemote(queryResult, 11, "dodo", "remote-host2", "path/b");
    }

    @Test
    public void findStatisticsFilterByRepoIncludeAll() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find({\"item.repo\" :\"repo1\"}).include(\"*\")");
        assertSize(queryResult, 5);
        assertStatistics(queryResult, 15, "yossis");
        assertStatistics(queryResult, 9, "yossis");
        assertStatisticsRemote(queryResult, 17, "dodo", "remote-host1", "path/a");
        assertStatisticsRemote(queryResult, 11, "dodo", "remote-host2", "path/b");
    }

    @Test
    public void findStatisticsFilterByRepoAndRemoteDownloadsIncludeAll() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find({\"item.repo\" :\"repo1\"},{\"remote_downloads\":{\"$gt\":11}}).include(\"*\")");
        assertSize(queryResult, 1);
        assertStatisticsRemote(queryResult, 17, "dodo", "remote-host1", "path/a");
    }

    @Test
    public void findRemoteStatistics() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find({\"item.repo\" :\"repo1\"},{\"remote_downloads\":{\"$gt\":0}})" +
                        ".include(\"remote_downloads\",\"remote_downloaded\",\"remote_downloaded_by\",\"remote_path\",\"remote_origin\")");
        // Returns 2 although there are 3 matching rows because distinct merges two identical rows.
        // Currently we don't see a valid use case where it matters.
        assertSize(queryResult, 2);
        assertStatisticsRemote(queryResult, 17, "dodo", "remote-host1", "path/a");
        assertStatisticsRemote(queryResult, 11, "dodo", "remote-host2", "path/b");
    }

    @Test
    public void findStatisticsIncludeSomeStatsAndItemFields() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find().include(\"downloads\",\"remote_downloads\",\"item.path\",\"item.repo\",\"item.name\",\"item.type\")");
        assertSize(queryResult, 5);
        assertStatistics(queryResult, 15, null);
        assertStatistics(queryResult, 9, null);
        assertStatisticsRemote(queryResult, 17, null, null, null);
        assertStatisticsRemote(queryResult, 11, null, null, null);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", AqlItemTypeEnum.file);
        assertItem(queryResult, "repo1", ".", "ant-launcher", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "ant-launcher", "ant-launcher", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "org", "yossis", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "org/yossis", "tools", AqlItemTypeEnum.folder);
    }

    @Test
    public void findItemIncludeRemoteStatistics() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\":\"any\",\"$or\":[{\"stat.downloads\":{\"$gt\":0}},{\"stat.remote_downloads\":{\"$gt\":0}}]})" +
                        ".include(\"stat.downloads\",\"stat.remote_downloads\",\"path\",\"repo\",\"name\",\"type\")");
        assertSize(queryResult, 5);
        assertStatistics(queryResult, 15, null);
        assertStatistics(queryResult, 9, null);
        assertStatisticsRemote(queryResult, 17, null, null, null);
        assertStatisticsRemote(queryResult, 11, null, null, null);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", AqlItemTypeEnum.file);
        assertItem(queryResult, "repo1", ".", "ant-launcher", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "ant-launcher", "ant-launcher", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "org", "yossis", AqlItemTypeEnum.folder);
        assertItem(queryResult, "repo1", "org/yossis", "tools", AqlItemTypeEnum.folder);
    }

    @Test
    public void findBuildsIncludeRemoteStatistics() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "builds.find({\"module.dependency.item.type\":\"any\"," +
                            "\"$or\":[{\"module.dependency.item.stat.downloads\":{\"$gt\":0}}," +
                                "{\"module.dependency.item.stat.remote_downloads\":{\"$gt\":0}}]})" +
                        ".include(\"*\",\"module.dependency.item.stat.downloads\",\"module.dependency.item.stat.remote_downloads\"," +
                            "\"module.dependency.item.path\",\"module.dependency.item.repo\",\"module.dependency.item.name\"," +
                            "\"module.dependency.item.type\")");
        assertSize(queryResult, 1);
        assertStatistics(queryResult, 9, null);
        assertStatisticsRemote(queryResult, 17, null, null, null);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", AqlItemTypeEnum.file);
    }

}


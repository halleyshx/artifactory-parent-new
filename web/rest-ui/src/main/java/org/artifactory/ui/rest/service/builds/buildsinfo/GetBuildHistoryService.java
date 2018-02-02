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

package org.artifactory.ui.rest.service.builds.buildsinfo;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.build.ArtifactBuildAddon;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.build.GeneralBuild;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.PagingData;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.PagingModel;
import org.artifactory.ui.rest.model.builds.GeneralBuildInfo;
import org.artifactory.ui.utils.ModelDbMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetBuildHistoryService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetBuildHistoryService.class);

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private BuildService buildService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        // get paging data from request
        PagingData pagingData = request.getPagingData();
        String buildName = request.getPathParamByKey("name");
        // fetch build info data
        fetchAllBuildsData(response, pagingData, buildName);
    }

    /**
     * fect all build data by type
     *
     * @param artifactoryResponse - encapsulate data related to response
     * @param pagingData          - paging data sa send from client
     */
    private void fetchAllBuildsData(RestResponse artifactoryResponse, PagingData pagingData, String buildName) {
        Map<String, String> buildsMap = ModelDbMap.getBuildsMap();
        String offset = pagingData.getStartOffset();
        String orderBy = buildsMap.get(pagingData.getOrderBy());
        String direction = pagingData.getDirection();
        String limit = pagingData.getLimit();
        try {
            List<GeneralBuild> latestBuildsPaging = buildService.getBuildForNamePaging(buildName, orderBy, direction,
                    offset, limit);
            if (latestBuildsPaging != null && !latestBuildsPaging.isEmpty()) {
                List<GeneralBuildInfo> generalBuildInfoList = new ArrayList<>();
                latestBuildsPaging.forEach(buildRun ->
                        generalBuildInfoList.add(new GeneralBuildInfo(new GeneralBuildInfo.BuildBuilder().buildNumber(buildRun.getBuildNumber())
                                        .lastBuildTime(centralConfigService.getDateFormatter().print(buildRun.getBuildDate())).
                                                releaseStatus(buildRun.getStatus())
                                        .ciUrl(buildRun.getCiUrl()).time(buildRun.getBuildDate()).
                                                time(buildRun.getBuildDate()).buildStat("Modules-" + buildRun.getNumOfModules() +
                                                ", Artifacts-" + buildRun.getNumOfArtifacts() + ", Dependencies-" + buildRun.getNumOfDependencies()))
                        ));
                PagingModel pagingModel = new PagingModel(0, generalBuildInfoList);
                artifactoryResponse.iModel(pagingModel);
            }
        } catch (SQLException e) {
            log.error(e.toString());
        }
    }
}

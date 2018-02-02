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

import org.artifactory.api.build.BuildService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.build.BuildId;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.PagingData;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.PagingModel;
import org.artifactory.ui.rest.model.builds.GeneralBuildInfo;
import org.artifactory.ui.utils.ModelDbMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAllBuildIDsService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private BuildService buildService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        // get paging data from request
        PagingData pagingData = request.getPagingData();
        // fetch build info data
        fetchAllBuildsData(response, pagingData);
    }

    /**
     * fect all build data by type
     *
     * @param artifactoryResponse - encapsulate data related to response
     * @param pagingData          - paging data sa send from client
     */
    private void fetchAllBuildsData(RestResponse artifactoryResponse, PagingData pagingData) {
        Map<String, String> buildsMap = ModelDbMap.getBuildsMap();
        String offset = pagingData.getStartOffset();
        String orderBy = buildsMap.get(pagingData.getOrderBy());
        String direction = pagingData.getDirection();
        String limit = pagingData.getLimit();
        Set<BuildId> latestBuildsPaging = buildService.getLatestBuildIDsPaging(offset, orderBy, direction, limit);
        if (latestBuildsPaging != null && !latestBuildsPaging.isEmpty()) {
            List<GeneralBuildInfo> generalBuildInfoList = new ArrayList<>();
            latestBuildsPaging.forEach(buildRun ->
                    generalBuildInfoList.add(new GeneralBuildInfo(new GeneralBuildInfo.BuildBuilder()
                            .buildName(buildRun.getName()).lastBuildTime(centralConfigService.getDateFormatter()
                                    .print(buildRun.getStartedDate().getTime())).buildNumber(buildRun.getNumber())
                                    .time(buildRun.getStartedDate() != null ? buildRun.getStartedDate().getTime() : 0))
                    ));
            PagingModel pagingModel = new PagingModel(0, generalBuildInfoList);
            artifactoryResponse.iModel(pagingModel);
        }
    }
}

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

package org.artifactory.ui.rest.service.builds.buildsinfo.tabs.env;

import org.artifactory.api.build.BuildProps;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.build.diff.BuildParams;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.PagingData;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.PagingModel;
import org.artifactory.ui.rest.model.builds.BuildPropsModel;
import org.artifactory.ui.utils.ModelDbMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetSystemBuildPropsService implements RestService {

    @Autowired
    BuildService buildService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String buildNumber = request.getPathParamByKey("number");
        String buildStarted = request.getPathParamByKey("date");
        PagingData pagingData = request.getPagingData();
        BuildParams buildParams = new BuildParams(null, buildNumber, null,
                null, buildStarted, null);
        Map<String, String> buildPropsMap = ModelDbMap.getBuildProps();
        List<BuildProps> buildPropsData = buildService.getBuildPropsData(buildParams, pagingData.getStartOffset(),
                pagingData.getLimit(), buildPropsMap.get(pagingData.getOrderBy()));
        if (!buildPropsData.isEmpty()) {
            List<BuildPropsModel> buildPropsModels = new ArrayList<>();
            buildPropsData.forEach(buildProps -> buildPropsModels.add(new BuildPropsModel(buildProps)));
            PagingModel pagingModel = new PagingModel(0, buildPropsModels);
            response.iModel(pagingModel);
        }
    }
}

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

package org.artifactory.ui.rest.service.builds.buildsinfo.tabs;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.build.BuildService;
import org.artifactory.build.BuildRun;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.ui.rest.model.builds.GeneralBuildInfo;
import org.artifactory.ui.utils.DateUtils;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.ParseException;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetBuildGeneralInfoService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetBuildGeneralInfoService.class);

    @Autowired
    private BuildService buildService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            String buildName = request.getPathParamByKey("name");
            String buildNumber = request.getPathParamByKey("number");
            String date = request.getPathParamByKey("date");
            String buildStarted = null;
            if (StringUtils.isNotBlank(date)) {
                buildStarted = DateUtils.formatBuildDate(Long.parseLong(date));
            }
            // get Build and update response
            getBuildAndUpdateResponse(response, buildName, buildNumber, buildStarted);
        } catch (ParseException e) {
            response.error("problem with fetching builds");
        }
    }

    /**
     * populate build info from build model and update response
     *
     * @param artifactoryResponse - encapsulate data require for response
     * @param buildName           - build name
     * @param buildNumber         - build number
     * @param buildStarted        - build start date
     */
    private void getBuildAndUpdateResponse(RestResponse artifactoryResponse, String buildName, String buildNumber,
            String buildStarted) {
        Build build = getBuild(buildName, buildNumber, buildStarted, artifactoryResponse);
        if (build == null) {
            // Already logged
            return;
        }
        Long time = null;
        try {
            buildStarted = buildStarted != null ? buildStarted : build.getStarted();
            time = DateUtils.toBuildDate(buildStarted);
        } catch (Exception e) {
            log.warn("Failed to parse the build started field: setting it as null.");
        }
        String buildAgent = (build.getBuildAgent() == null) ? null : build.getBuildAgent().toString();
        String agent = (build.getAgent() == null) ? null : build.getAgent().toString();
        GeneralBuildInfo generalBuildInfo = new GeneralBuildInfo(new GeneralBuildInfo.BuildBuilder()
                .buildName(build.getName())
                .lastBuildTime(buildStarted)
                .agent(agent)
                .buildAgent(buildAgent)
                .artifactoryPrincipal(build.getArtifactoryPrincipal())
                .principal(build.getPrincipal())
                .duration(DateUtils.getDuration(build.getDurationMillis()))
                .buildNumber(buildNumber)
                .time(time)
                .url(build.getUrl()));
        artifactoryResponse.iModel(generalBuildInfo);
    }

    /**
     * get build model
     *
     * @param buildName    - build name
     * @param buildNumber  - build number
     * @param buildStarted - build start date
     * @param response     - encapsulate data require for response
     */
    private Build getBuild(String buildName, String buildNumber, String buildStarted, RestResponse response) {
        boolean buildStartedSupplied = StringUtils.isNotBlank(buildStarted);
        Build build = null;
        try {
            build = getBuild(buildName, buildNumber, buildStarted, buildStartedSupplied);
            if (build == null) {
                StringBuilder builder = new StringBuilder().append("Could not find build '").append(buildName).
                        append("' #").append(buildNumber);
                if (buildStartedSupplied) {
                    builder.append(" that started at ").append(buildStarted);
                }
                response.error(builder.toString());
            }
        } catch (RepositoryRuntimeException e) {
            String errorMessage = "Error locating latest build for '" + buildName + "' #" + buildNumber + ": "
                    + e.getMessage();
            response.error(errorMessage);
        }
        return build;
    }

    /**
     * get build general info
     *
     * @param buildName            - build n name
     * @param buildNumber          - build number
     * @param buildStarted         - build started date
     * @param buildStartedSupplied - build started supplier
     * @return - build model
     */
    private Build getBuild(String buildName, String buildNumber, String buildStarted, boolean buildStartedSupplied) {
        Build build = null;
        if (buildStartedSupplied) {
            BuildRun buildRun = buildService.getBuildRun(buildName, buildNumber, buildStarted);
            if (buildRun != null) {
                build = buildService.getBuild(buildRun);
            }
        } else {
            //Take the latest build of the specified number
            build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        }
        return build;
    }
}

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

package org.artifactory.ui.rest.service.admin.configuration.repositories.util.validator;

import com.google.common.collect.Maps;
import org.apache.http.HttpStatus;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.util.exception.RepoConfigException;
import org.artifactory.ui.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Provides the entire validation logic for a repository key for the ui in a single REST call
 *
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateRepoNameService implements RestService {

    @Autowired
    private RepoConfigValidator repoConfigValidator;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String repoKey = RequestUtils.getRepoKeyFromRequest(request);
        boolean isRemote = Boolean.parseBoolean(request.getQueryParamByKey("remote"));
        try {
            repoConfigValidator.validateRepoName(repoKey, isRemote);
        } catch (RepoConfigException e) {
            Map<String, String> errorMap = Maps.newHashMap();
            errorMap.put("error", e.getMessage());
            response.iModel(errorMap);
        }
        //Always return OK for ui validation
        response.responseCode(HttpStatus.SC_OK);
    }
}
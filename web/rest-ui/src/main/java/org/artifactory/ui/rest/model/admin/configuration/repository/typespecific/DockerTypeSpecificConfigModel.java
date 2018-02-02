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

package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.DockerApiVersion;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.*;

/**
 * @author Dan Feldman
 */
public class DockerTypeSpecificConfigModel implements TypeSpecificConfigModel {
    
    //local
    protected DockerApiVersion dockerApiVersion = DEFAULT_DOCKER_API_VER;
    protected int maxUniqueTags = 0;

    //remote
    protected Boolean enableTokenAuthentication = DEFAULT_TOKEN_AUTH;
    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;


    public DockerApiVersion getDockerApiVersion() {
        return dockerApiVersion;
    }

    public void setDockerApiVersion(DockerApiVersion dockerApiVersion) {
        this.dockerApiVersion = dockerApiVersion;
    }

    public int getMaxUniqueTags() {
        return maxUniqueTags;
    }

    public void setMaxUniqueTags(int maxUniqueTags) {
        this.maxUniqueTags = maxUniqueTags;
    }

    public Boolean isEnableTokenAuthentication() {
        return enableTokenAuthentication;
    }

    public void setEnableTokenAuthentication(Boolean enableTokenAuthentication) {
        this.enableTokenAuthentication = enableTokenAuthentication;
    }

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Docker;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.DOCKER_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

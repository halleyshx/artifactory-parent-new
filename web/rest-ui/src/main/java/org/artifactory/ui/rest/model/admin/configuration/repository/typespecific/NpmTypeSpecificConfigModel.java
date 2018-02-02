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

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;

import java.util.List;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

/**
 * @author Dan Feldman
 */
public class NpmTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //virtual
    private Boolean enableExternalDependencies = false;
    private List<String> externalPatterns = Lists.newArrayList("**");
    private String externalRemoteRepo = "";

    public Boolean getEnableExternalDependencies() {
        return enableExternalDependencies;
    }

    public void setEnableExternalDependencies(Boolean enableExternalDependencies) {
        this.enableExternalDependencies = enableExternalDependencies;
    }

    public List<String> getExternalPatterns() {
        return externalPatterns;
    }

    public void setExternalPatterns(List<String> externalPatterns) {
        this.externalPatterns = externalPatterns;
    }

    public String getExternalRemoteRepo() {
        return externalRemoteRepo;
    }

    public void setExternalRemoteRepo(String externalRemoteRepo) {
        this.externalRemoteRepo = externalRemoteRepo;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Npm;
    }

    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.NPM_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

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

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.VcsGitConfiguration;
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_PODS_SPECS_REPO;
import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_VCS_GIT_CONFIG;

/**
 * @author Dan Feldman
 */
public class CocoaPodsTypeSpecificConfigModel extends VcsTypeSpecificConfigModel {

    //remote
    private String specsRepoUrl = DEFAULT_PODS_SPECS_REPO;
    private VcsGitConfiguration specsRepoProvider = DEFAULT_VCS_GIT_CONFIG;

    //TODO [by dan]: ?
    //virtual
    //private Boolean enableExternalDependencies = false;
    //private List<String> externalPatterns = Lists.newArrayList("**");
    //private String externalRemoteRepo = "";

    public String getSpecsRepoUrl() {
        return specsRepoUrl;
    }

    public void setSpecsRepoUrl(String specsRepoUrl) {
        this.specsRepoUrl = specsRepoUrl;
    }

    public VcsGitConfiguration getSpecsRepoProvider() {
        return specsRepoProvider;
    }

    public void setSpecsRepoProvider(VcsGitConfiguration specsRepoProvider) {
        this.specsRepoProvider = specsRepoProvider;
    }

    /*  public Boolean getEnableExternalDependencies() {
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
    }*/

    @Override
    public RepoType getRepoType() {
        return RepoType.CocoaPods;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.VCS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

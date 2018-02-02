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
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.*;

/**
 * @author Dan Feldman
 */
public class NugetTypeSpecificConfigModel implements TypeSpecificConfigModel {

    protected Boolean forceNugetAuthentication = DEFAULT_FORCE_NUGET_AUTH;

    //local
    protected Integer maxUniqueSnapshots = DEFAULT_MAX_UNIQUE_SNAPSHOTS;

    //remote
    protected String feedContextPath = DEFAULT_NUGET_FEED_PATH;
    protected String downloadContextPath = DEFAULT_NUGET_DOWNLOAD_PATH;
    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

    public String getFeedContextPath() {
        return feedContextPath;
    }

    public void setFeedContextPath(String feedContextPath) {
        this.feedContextPath = feedContextPath;
    }

    public String getDownloadContextPath() {
        return downloadContextPath;
    }

    public void setDownloadContextPath(String downloadContextPath) {
        this.downloadContextPath = downloadContextPath;
    }

    public Integer getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setMaxUniqueSnapshots(Integer maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    public Boolean isForceNugetAuthentication() {
        return forceNugetAuthentication;
    }

    public void setForceNugetAuthentication(Boolean forceNugetAuthentication) {
        this.forceNugetAuthentication = forceNugetAuthentication;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.NuGet;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.NUGET_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

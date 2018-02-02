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

import java.util.List;

import static org.artifactory.repo.config.RepoConfigDefaultValues.*;

/**
 * @author Dan Feldman
 */
public class DebTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //local
    protected Boolean trivialLayout = DEFAULT_DEB_TRIVIAL_LAYOUT;

    public List<String> optionalIndexCompressionFormats = getDefaultDebianPackagesFileFormats();

    //remote
    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_SUPPORTED_TYPE;

    public Boolean getTrivialLayout() {
        return trivialLayout;
    }

    public void setTrivialLayout(Boolean trivialLayout) {
        this.trivialLayout = trivialLayout;
    }

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }
    public void setOptionalIndexCompressionFormats(List<String> fileArchiveFormats) {
        this.optionalIndexCompressionFormats = fileArchiveFormats;
    }
    public List<String> getOptionalIndexCompressionFormats() {
        return this.optionalIndexCompressionFormats;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Debian;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.DEBIAN_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

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

package org.artifactory.ui.rest.service.admin.security.permissions;

import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.permissions.PermissionTargetModel;
import org.artifactory.ui.rest.model.utils.repositories.RepoKeyType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
public abstract class BasePermissionsTargetService<T> implements RestService<T> {

    /**
     * filter repo keys based on if any remote or any local has been choose
     *
     * @param permissionTarget - permission target model
     */
    protected void filteredRepoKey(PermissionTargetModel permissionTarget) {
        List<RepoKeyType> filteredRepoKeys = new ArrayList<>();
        if (permissionTarget.isAnyLocal() && permissionTarget.isAnyRemote() && permissionTarget.isAnyDistribution()) {
            filteredRepoKeys.add(new RepoKeyType("ANY", "ANY"));
        } else if (permissionTarget.isAnyLocal() && !permissionTarget.isAnyRemote()) {
            List<RepoKeyType> repoKeys = permissionTarget.getRepoKeys();
            repoKeys.stream()
                    .filter(repoKeyType -> repoKeyType.getType().equals("remote") || repoKeyType.getType().equals("distribution"))
                    .forEach(filteredRepoKeys::add);
            filteredRepoKeys.add(new RepoKeyType("ANY LOCAL", "ANY LOCAL"));
        } else if (!permissionTarget.isAnyLocal() && permissionTarget.isAnyRemote()) {
            List<RepoKeyType> repoKeys = permissionTarget.getRepoKeys();
            repoKeys.stream()
                    .filter(repoKeyType -> repoKeyType.getType().equals("local") || repoKeyType.getType().equals("distribution"))
                    .forEach(filteredRepoKeys::add);
            filteredRepoKeys.add(new RepoKeyType("ANY REMOTE", "ANY REMOTE"));
        } else if (permissionTarget.isAnyLocal() && permissionTarget.isAnyRemote()) {
            List<RepoKeyType> repoKeys = permissionTarget.getRepoKeys();
            repoKeys.stream()
                    .filter(repoKeyType -> repoKeyType.getType().equals("distribution"))
                    .forEach(filteredRepoKeys::add);
            filteredRepoKeys.add(new RepoKeyType("ANY LOCAL", "ANY LOCAL"));
            filteredRepoKeys.add(new RepoKeyType("ANY REMOTE", "ANY REMOTE"));
        }
        if (!filteredRepoKeys.isEmpty()) {
            permissionTarget.setRepoKeys(filteredRepoKeys);
        }
    }
}

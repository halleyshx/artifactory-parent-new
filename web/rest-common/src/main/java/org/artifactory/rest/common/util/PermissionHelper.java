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

package org.artifactory.rest.common.util;

import com.sun.jersey.api.NotFoundException;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.exception.ForbiddenException;
import org.artifactory.security.ArtifactoryPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper that check if a user has permission on a repo. In case of virtual, it checks if a user has permission on
 * All of the virtual aggregated repos
 *
 * @author Shay Bagants
 */
@Component
public class PermissionHelper {

    @Autowired
    private RepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

    public void assertPermission(String repositoryPath, ArtifactoryPermission permission) {
        RepoPath repoPath = RepoPathFactory.create(repositoryPath);
        String repoKey = repoPath.getRepoKey();
        RepoDescriptor repoDescriptor = repoService.repoDescriptorByKey(repoKey);
        if (repoDescriptor == null) {
            throw new NotFoundException("Repository '" + repoKey + "' not found");
        }

        if (repoDescriptor.isReal()) {
            assertPermissionForRealRepo(repoPath, permission);
        } else {
            assertPermissionForVirtual(repoPath, permission);
        }
    }

    private void assertPermissionForVirtual(RepoPath repoPath, ArtifactoryPermission permission) {
        repoService.getVirtualResolvedLocalAndCacheDescriptors(repoPath.getRepoKey())
                .forEach(localRepoDescriptor -> {
                    RepoPath pathToCheck = RepoPathFactory.create(localRepoDescriptor.getKey(), repoPath.getPath());
                    assertPermissionForRealRepo(pathToCheck, permission);
                });
    }

    private void assertPermissionForRealRepo(RepoPath repoPath, ArtifactoryPermission permission) {
        boolean hasPermission = false;
        switch (permission) {
            case READ:
                hasPermission = authService.canRead(repoPath);
                break;
            case ANNOTATE:
                hasPermission = authService.canAnnotate(repoPath);
                break;
            case DEPLOY:
                hasPermission = authService.canDeploy(repoPath);
                break;
            case DELETE:
                hasPermission = authService.canDelete(repoPath);
                break;
            case MANAGE:
                hasPermission = authService.canManage(repoPath);
                break;
        }
        if (!hasPermission) {
            throw new ForbiddenException();
        }
    }
}

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

package org.artifactory.addon.debian;

import org.artifactory.addon.Addon;
import org.artifactory.addon.debian.index.DebianCalculationEvent;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.repo.RepoPath;

import java.util.Set;

/**
 * @author Gidi Shabat
 */
public interface DebianAddon extends Addon {

    /**
     * Calculate all the indices in repo
     * the method is being invoked by the recalculate button in the UI or by REST
     * Note that the REST clients are allowed to pass password by headers
     */
    void recalculateAll(LocalRepoDescriptor descriptor, String passphrase, boolean delayed);

    void calculateMetadata(Set<DebianCalculationEvent> calculationRequests, boolean delayed);

    boolean hasPrivateKey();

    boolean hasPublicKey();

    boolean foundExpiredAndRemoteIsNewer(RepoResource remoteResource, RepoResource cachedResource);

    /**
     * Used to get the package metadata for the UI info tab
     *
     * @param repoPath Path to the package
     * @return DebianMetadataInfo instance - UI model for the info tab
     */
    DebianMetadataInfo getDebianMetadataInfo(RepoPath repoPath);
}

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

package org.artifactory.rest.services.replication;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.MissingRestAddonException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.repo.Repo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.services.IService;
import org.artifactory.util.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Chen Keinan
 */
public abstract class BaseReplicationService implements IService {

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    protected CentralConfigService centralConfigService;

    protected void verifyArtifactoryPro() {
        if (addonsManager.addonByType(RestAddon.class).isDefault()) {
            throw new MissingRestAddonException();
        }
    }

    protected void verifyArtifactoryEnterprise(){
        if (!addonsManager.isHaLicensed()) {
            throw new AuthorizationRestException(
                    "Multi-push replication is only available with an Enterprise license.");
        }
    }

    protected Repo verifyRepositoryExists(String repoKey) {
        Repo repo = repositoryByKey(repoKey);
        if (repo == null) {
            throw new BadRequestException("Could not find repository");
        }
        return repo;
    }

    protected Repo repositoryByKey(String repoKey) {
        return ((InternalRepositoryService) ContextHelper.get().getRepositoryService()).repositoryByKey(repoKey);
    }

    /**
     * add or replace replication
     *
     * @param newReplication - new replication
     * @param replications   - list of replications
     * @param <T>            - new replication
     */
    protected <T extends ReplicationBaseDescriptor> void addOrReplace(T newReplication, List<T> replications) {
        String repoKey = newReplication.getRepoKey();
        T existingReplication = getReplication(repoKey, replications);

        saveReplication(replications, newReplication, existingReplication, repoKey,
                newReplication instanceof LocalReplicationDescriptor ? ((LocalReplicationDescriptor) newReplication).getUrl() : null);
    }

    protected <T extends ReplicationBaseDescriptor> void saveReplication(List<T> replications, T newReplication,
            T existingReplication,
            String repoKey, String url) {
        if (existingReplication != null) {
            newReplication.setReplicationKey(StringUtils.isBlank(existingReplication.getReplicationKey()) ?
                    IdUtils.produceReplicationId(repoKey, url) :
                    existingReplication.getReplicationKey());
            int i = replications.indexOf(existingReplication);
            replications.set(i, newReplication); //replace
        } else {
            newReplication.setReplicationKey(IdUtils.produceReplicationId(repoKey, url));
            replications.add(newReplication); //add
        }
    }

    /**
     * get replication
     *
     * @param <T>               replication base descriptor
     * @param replicatedRepoKey - replication repo key
     * @param replications      - list of replications
     * @return replication descriptor
     */
    protected <T extends ReplicationBaseDescriptor> T getReplication(String replicatedRepoKey, List<T> replications) {
        for (T replication : replications) {
            if (replicatedRepoKey.equals(replication.getRepoKey())) {
                return replication;
            }
        }
        return null;
    }
}

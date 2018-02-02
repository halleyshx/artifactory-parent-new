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

package org.artifactory.layout;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.webstart.ArtifactWebstartAddon;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.ConfigurationChangesInterceptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fred Simon
 */
@Component
public class RepositoriesConfigurationChangesInterceptor implements ConfigurationChangesInterceptor {

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void onBeforeSave(CentralConfigDescriptor newDescriptor) {
        if (newDescriptor instanceof MutableCentralConfigDescriptor) {
            MutableCentralConfigDescriptor mutableCentralConfigDescriptor = (MutableCentralConfigDescriptor) newDescriptor;
            cleanClientCertificateIfDeleted(newDescriptor, mutableCentralConfigDescriptor);
            orderReposIfNeeded(newDescriptor, mutableCentralConfigDescriptor);
        }
    }

    private void orderReposIfNeeded(CentralConfigDescriptor newDescriptor,
            MutableCentralConfigDescriptor mutableCentralConfigDescriptor) {
        if (ConstantValues.disableGlobalRepoAccess.getBoolean()) {
            // If global repo is disabled, all repository key names are ordered
            mutableCentralConfigDescriptor.setRemoteRepositoriesMap(sortMap(newDescriptor.getRemoteRepositoriesMap()));
            mutableCentralConfigDescriptor.setLocalRepositoriesMap(sortMap(newDescriptor.getLocalRepositoriesMap()));
            mutableCentralConfigDescriptor.setVirtualRepositoriesMap(sortMap(newDescriptor.getVirtualRepositoriesMap()));
        }
    }

    private void cleanClientCertificateIfDeleted(CentralConfigDescriptor newDescriptor,
            MutableCentralConfigDescriptor mutableCentralConfigDescriptor) {
        Map<String, RemoteRepoDescriptor> remoteRepos = newDescriptor.getRemoteRepositoriesMap();
        remoteRepos.forEach((key, descriptor) -> {
            if (descriptor instanceof HttpRepoDescriptor) {
                String clientCert = ((HttpRepoDescriptor) descriptor).getClientTlsCertificate();
                if (StringUtils.isNotBlank(clientCert)) {
                    ArtifactWebstartAddon webstartAddon = addonsManager.addonByType(ArtifactWebstartAddon.class);

                    List<String> certAliases = webstartAddon.getSslCertNames();
                    if (certAliases.stream().noneMatch(clientCert::equalsIgnoreCase)) {
                        ((HttpRepoDescriptor) descriptor).setClientTlsCertificate(null);
                    }
                }
            }
        });
        mutableCentralConfigDescriptor.setRemoteRepositoriesMap(remoteRepos);
    }

    private <T extends RepoDescriptor> Map<String, T> sortMap(Map<String, T> map) {
        String[] origKeys = map.keySet().toArray(new String[map.size()]);
        String[] orderedKeys = map.keySet().toArray(new String[map.size()]);
        Arrays.sort(orderedKeys);
        if (Arrays.equals(origKeys, orderedKeys)) {
            return map;
        } else {
            Map<String, T> result = new LinkedHashMap<>(map.size());
            for (String orderedKey : orderedKeys) {
                result.put(orderedKey, map.get(orderedKey));
            }
            return result;
        }
    }

    @Override
    public void onAfterSave(CentralConfigDescriptor newDescriptor, CentralConfigDescriptor oldDescriptor) {

    }
}

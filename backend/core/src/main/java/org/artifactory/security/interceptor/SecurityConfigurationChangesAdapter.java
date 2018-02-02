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

package org.artifactory.security.interceptor;

import org.artifactory.security.AclInfo;
import org.artifactory.security.SecurityInfo;

import java.util.List;

/**
 * A default implementation adapter for the security configuration changes interceptors
 *
 * @author Noam Y. Tenne
 */
public abstract class SecurityConfigurationChangesAdapter implements SecurityConfigurationChangesInterceptor {

    @Override
    public void onUserAdd(String user) {
    }

    @Override
    public void onUserDelete(String user) {
    }

    @Override
    public void onAddUsersToGroup(String groupName, List<String> usernames) {
    }

    @Override
    public void onRemoveUsersFromGroup(String groupName, List<String> usernames) {
    }

    @Override
    public void onGroupAdd(String group) {
    }

    @Override
    public void onGroupDelete(String group) {
    }

    @Override
    public void onPermissionsAdd(AclInfo added) {
    }

    @Override
    public void onPermissionsUpdate(AclInfo updated) {
    }

    @Override
    public void onPermissionsDelete(String deleted) {
    }

    @Override
    public void onBeforeSecurityImport(SecurityInfo securityInfo) {
    }
}
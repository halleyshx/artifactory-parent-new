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

package org.artifactory.ui.rest.model.admin.security.ldap;

import java.util.List;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class LdapImportModel extends BaseModel {

    private LdapGroupModel ldapGroupSettings;
    private List<LdapUserGroupModel> importGroups;

    public LdapGroupModel getLdapGroupSettings() {
        return ldapGroupSettings;
    }

    public void setLdapGroupSettings(LdapGroupModel ldapGroupSettings) {
        this.ldapGroupSettings = ldapGroupSettings;
    }

    public List<LdapUserGroupModel> getImportGroups() {
        return importGroups;
    }

    public void setImportGroups(
            List<LdapUserGroupModel> importGroups) {
        this.importGroups = importGroups;
    }
}

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

package org.artifactory.descriptor.security.sso;

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The HTTP SSO related settings
 *
 * @author Noam Y. Tenne
 */
@XmlType(name = "HttpSsoSettingsType",
        propOrder = {"httpSsoProxied", "noAutoUserCreation", "allowUserToAccessProfile", "remoteUserRequestVariable", "syncLdapGroups"}, namespace = Descriptor.NS)
@GenerateDiffFunction
public class HttpSsoSettings implements Descriptor {

    @XmlElement(defaultValue = "false")
    private boolean httpSsoProxied = false;

    @XmlElement(defaultValue = "false")
    private boolean noAutoUserCreation = false;

    @XmlElement(defaultValue = "false")
    private boolean allowUserToAccessProfile = false;

    @XmlElement(defaultValue = "REMOTE_USER")
    private String remoteUserRequestVariable = "REMOTE_USER";

    @XmlElement(defaultValue = "false")
    private boolean syncLdapGroups = false;

    public boolean isHttpSsoProxied() {
        return httpSsoProxied;
    }

    public void setHttpSsoProxied(boolean httpSsoProxied) {
        this.httpSsoProxied = httpSsoProxied;
    }

    public boolean isNoAutoUserCreation() {
        return noAutoUserCreation;
    }

    public void setNoAutoUserCreation(boolean noAutoUserCreation) {
        this.noAutoUserCreation = noAutoUserCreation;
    }

    public boolean isAllowUserToAccessProfile() {
        return allowUserToAccessProfile;
    }

    public void setAllowUserToAccessProfile(boolean allowUserToAccessProfile) {
        this.allowUserToAccessProfile = allowUserToAccessProfile;
    }

    public String getRemoteUserRequestVariable() {
        return remoteUserRequestVariable;
    }

    public void setRemoteUserRequestVariable(String remoteUserRequestVariable) {
        this.remoteUserRequestVariable = remoteUserRequestVariable;
    }

    public boolean isSyncLdapGroups() {
        return syncLdapGroups;
    }

    public void setSyncLdapGroups(boolean syncLdapGroups) {
        this.syncLdapGroups = syncLdapGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HttpSsoSettings that = (HttpSsoSettings) o;

        if (httpSsoProxied != that.httpSsoProxied) {
            return false;
        }
        if (noAutoUserCreation != that.noAutoUserCreation) {
            return false;
        }
        if (allowUserToAccessProfile != that.allowUserToAccessProfile) {
            return false;
        }
        if (syncLdapGroups != that.syncLdapGroups) {
            return false;
        }
        return remoteUserRequestVariable != null ? remoteUserRequestVariable.equals(that.remoteUserRequestVariable) :
                that.remoteUserRequestVariable == null;
    }

    @Override
    public int hashCode() {
        int result = (httpSsoProxied ? 1 : 0);
        result = 31 * result + (noAutoUserCreation ? 1 : 0);
        result = 31 * result + (allowUserToAccessProfile ? 1 : 0);
        result = 31 * result + (remoteUserRequestVariable != null ? remoteUserRequestVariable.hashCode() : 0);
        result = 31 * result + (syncLdapGroups ? 1 : 0);
        return result;
    }
}
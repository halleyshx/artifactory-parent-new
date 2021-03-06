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

package org.artifactory.descriptor.replication;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.jfrog.common.DiffReference;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Noam Y. Tenne
 */
@XmlType(name = "LocalReplicationType", propOrder = {"url", "proxy", "socketTimeoutMillis", "username", "password",
        "syncStatistics"}, namespace = Descriptor.NS)
@GenerateDiffFunction
public class LocalReplicationDescriptor extends ReplicationBaseDescriptor {

    @XmlElement(required = false)
    private String url;

    @XmlIDREF
    @XmlElement(name = "proxyRef", required = false)
    private ProxyDescriptor proxy;

    @XmlElement(defaultValue = "15000", required = false)
    private int socketTimeoutMillis = 15000;//Default socket timeout

    @XmlElement(required = false)
    private String username;

    @XmlElement(required = false)
    private String password;

    @XmlElement(defaultValue = "false")
    private boolean syncStatistics = false;

    @DiffReference
    public ProxyDescriptor getProxy() {
        return proxy;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setProxy(ProxyDescriptor proxy) {
        this.proxy = proxy;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public void setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSyncStatistics() {
        return syncStatistics;
    }

    public void setSyncStatistics(boolean syncStatistics) {
        this.syncStatistics = syncStatistics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        LocalReplicationDescriptor that = (LocalReplicationDescriptor) o;

        if (socketTimeoutMillis != that.socketTimeoutMillis) {
            return false;
        }
        if (syncStatistics != that.syncStatistics) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (proxy != null ? !proxy.equals(that.proxy) : that.proxy != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        return password != null ? password.equals(that.password) : that.password == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (proxy != null ? proxy.hashCode() : 0);
        result = 31 * result + socketTimeoutMillis;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (syncStatistics ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LocalReplication[" + getRepoKey() + "|" + url + "|" + getReplicationKey() + "]";
    }
}

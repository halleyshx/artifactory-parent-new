/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "XrayType", propOrder = {"enabled", "baseUrl", "user", "password", "artifactoryId", "xrayId"})
@GenerateDiffFunction
public class XrayDescriptor implements Descriptor {

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;
    @XmlElement(name = "baseUrl", required = true)
    private String baseUrl;
    @XmlElement(name = "user", required = true)
    private String user;
    @XmlElement(name = "password", required = true)
    private String password;
    @XmlElement(name = "artifactoryId", required = true)
    private String artifactoryId;
    @XmlElement(name = "xrayId", required = true)
    private String xrayId;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getArtifactoryId() {
        return artifactoryId;
    }

    public void setArtifactoryId(String artifactoryId) {
        this.artifactoryId = artifactoryId;
    }

    public String getXrayId() {
        return xrayId;
    }

    public void setXrayId(String xrayId) {
        this.xrayId = xrayId;
    }
}

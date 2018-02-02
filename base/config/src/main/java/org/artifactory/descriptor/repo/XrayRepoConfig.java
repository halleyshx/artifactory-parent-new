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

package org.artifactory.descriptor.repo;

import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Xray specific configuration for repositories
 *
 * @author Dan Feldman
 */
@XmlType(name = "RepoXrayConfigType", propOrder = {"enabled", "minimumBlockedSeverity", "blockUnscannedArtifacts"},
        namespace = Descriptor.NS)
@GenerateDiffFunction
public class XrayRepoConfig implements Descriptor {

    @XmlElement(defaultValue = "false", required = true)
    private boolean enabled = false;

    @XmlElement(required = false)
    private String minimumBlockedSeverity;

    @XmlElement(defaultValue = "false", required = true)
    private boolean blockUnscannedArtifacts = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMinimumBlockedSeverity() {
        return minimumBlockedSeverity;
    }

    public void setMinimumBlockedSeverity(String minimumBlockedSeverity) {
        this.minimumBlockedSeverity = minimumBlockedSeverity;
    }

    public boolean isBlockUnscannedArtifacts() {
        return blockUnscannedArtifacts;
    }

    public void setBlockUnscannedArtifacts(boolean blockUnscannedArtifacts) {
        this.blockUnscannedArtifacts = blockUnscannedArtifacts;
    }

    public boolean repoBlocksDownloads() {
        return StringUtils.isNotEmpty(minimumBlockedSeverity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XrayRepoConfig)) {
            return false;
        }

        XrayRepoConfig that = (XrayRepoConfig) o;

        if (isEnabled() != that.isEnabled()) {
            return false;
        }
        if (isBlockUnscannedArtifacts() != that.isBlockUnscannedArtifacts()) {
            return false;
        }
        return getMinimumBlockedSeverity() != null ?
                getMinimumBlockedSeverity().equals(that.getMinimumBlockedSeverity()) :
                that.getMinimumBlockedSeverity() == null;

    }

    @Override
    public int hashCode() {
        int result = (isEnabled() ? 1 : 0);
        result = 31 * result + (getMinimumBlockedSeverity() != null ? getMinimumBlockedSeverity().hashCode() : 0);
        result = 31 * result + (isBlockUnscannedArtifacts() ? 1 : 0);
        return result;
    }
}
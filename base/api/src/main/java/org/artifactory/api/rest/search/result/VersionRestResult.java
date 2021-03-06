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

package org.artifactory.api.rest.search.result;

import org.artifactory.api.config.VersionInfo;

import java.util.List;

/**
 * A wrapper class for a JSON object representation of Artifactory's version, revision, and a list of enabled addons.
 *
 * @author Tomer Cohen
 */
public class VersionRestResult {

    public String version;
    public String revision;
    public List<String> addons;
    public String license;

    public VersionRestResult(String version, String revision, List<String> addons, String license) {
        this.version = version;
        this.revision = revision;
        this.addons = addons;
        this.license = license;
    }

    /**
     * Constructor used by the JSON parser
     */
    private VersionRestResult() {

    }

    /**
     * Converts {@link VersionRestResult} to {@link VersionInfo}
     *
     * @return {@link VersionInfo}
     */
    public VersionInfo toVersionInfo() {
        return new VersionInfo(version, revision);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("version: ");
        sb.append(version);
        sb.append(" | ");
        sb.append("revision: ");
        sb.append(revision);
        sb.append(" | ");
        sb.append("addons: ");
        sb.append(addons);
        sb.append(" | ");
        sb.append("license: ");
        sb.append(license);
        return sb.toString();
    }
}

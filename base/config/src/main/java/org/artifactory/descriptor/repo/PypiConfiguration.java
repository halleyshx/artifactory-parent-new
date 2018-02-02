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

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Yoav Luft
 */
@XmlType(name = "PypiConfigurationType", propOrder = {"indexContextPath", "packagesContextPath"})
@GenerateDiffFunction
public class PypiConfiguration implements Descriptor {

    @XmlElement(defaultValue = "", required = false)
    private String indexContextPath;

    @XmlElement(defaultValue = "", required = false)
    private String packagesContextPath;

    public String getPackagesContextPath() {
        return packagesContextPath;
    }

    public void setPackagesContextPath(@Nonnull String packagesContextPath) {
        this.packagesContextPath = packagesContextPath;
    }

    public String getIndexContextPath() {
        return indexContextPath;
    }

    public void setIndexContextPath(@Nonnull String indexContextPath) {
        this.indexContextPath = indexContextPath;
    }
}

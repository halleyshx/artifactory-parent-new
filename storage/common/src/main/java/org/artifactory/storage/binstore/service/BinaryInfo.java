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

package org.artifactory.storage.binstore.service;

import java.io.Serializable;

/**
 * Date: 11/26/12
 * Time: 10:24 AM
 *
 * @author freds
 */
//TODO [by dan]: REFACTOR REQUIRED IN PHASE 2: BinaryInfo class is to be dropped and usages switched to the BinaryElement class
//TODO [by dan]: make the BinaryElementImpl class only have an EnumMap of headers and getter methods will retrieve values
//TODO [by dan]: from it (should be fast enough) --> users of BinaryElement interface will need a way to access the
//TODO [by dan]: factory methods in BinaryProviderManager (via util or the service itself)
@Deprecated
public interface BinaryInfo extends Serializable {

    String getSha1();

    String getSha2();

    String getMd5();

    long getLength();
}

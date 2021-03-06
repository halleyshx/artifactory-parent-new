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

package org.artifactory.sapi.data;

import java.util.Calendar;
import java.util.Collection;

/**
 * A property value container for any VfsNode.
 * We support auto multi values for strings.
 * We don't support multi values for long and date.
 * <p/>
 * Date: 8/4/11
 * Time: 9:06 AM
 *
 * @author Fred Simon
 */
public interface VfsProperty {

    public enum VfsValueType {STRING, LONG, DATE}

    public enum VfsPropertyType {AUTO, SINGLE, MULTI_VALUE}

    VfsValueType getValueType();

    VfsPropertyType getPropertyType();

    String getString();

    Collection<String> getStrings();

    Long getLong();

    Calendar getDate();
}

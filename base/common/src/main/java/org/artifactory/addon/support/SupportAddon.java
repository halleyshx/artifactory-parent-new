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

package org.artifactory.addon.support;

import org.artifactory.addon.Addon;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Michael Pasternak
 */
public interface SupportAddon extends Addon {

    boolean isSupportAddonEnabled();

    /**
     * Generates support bundle/s
     *
     * @param bundleConfiguration config to be used
     *
     * @return name/s of generated bundles
     */
    List<String> generate(Object bundleConfiguration);

    /**
     * List earlier created support bundle/s. In case of HA, aggregate the list from all the cluster members
     *
     * @return name/s of generated bundles
     */
    List<String> list();

    /**
     * List earlier created support bundle/s from the this server only.
     *
     * @return name/s of generated bundles
     */
    List<String> listFromThisServer();

    /**
     * Downloads support bundles
     *
     * @param bundleName
     * @return {@link InputStream} to support bundle
     *
     * @throws FileNotFoundException
     */
    InputStream download(String bundleName, String handlingNode) throws FileNotFoundException;

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async whether delete should be performed asynchronously
     *
     * @return result
     */
    boolean delete(String bundleName, boolean shouldPropagate, boolean async);
}

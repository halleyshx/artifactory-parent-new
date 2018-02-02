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

package org.artifactory.support.core.bundle;

import org.artifactory.support.config.bundle.BundleConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Defines bundle generator behaviour
 *
 * @author Michael Pasternak
 */
public interface SupportBundleService {
    String SUPPORT_BUNDLE_PREFIX = "support-bundle-";
    Pattern BUNDLE_PATTERN = Pattern.compile("support-bundle-([\\d]*\\-[\\d]*\\-[\\d]*)\\.zip");

    /**
     * Generates support bundle
     *
     * @param bundleConfiguration config to be used
     *
     * @return compressed archive/s
     */
    List<String> generate(BundleConfiguration bundleConfiguration);

    /**
     * Lists previously created bundles. In case of HA, return list of all cluster members.
     *
     * @return archive/s
     */
    List<String> list();

    /**
     * Lists previously created bundles from this server only.
     *
     * @return archive/s
     */
    List<String> listFromThisServer();

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async whether delete should be performed asynchronously
     *
     * @return result
     */
    boolean delete(String bundleName, boolean shouldPropagate, boolean async);

    /**
     * Downloads support bundles
     *
     * @param bundleName
     * @param handlingNode The node that should handle the request
     * @return {@link InputStream} to support bundle
     *         (user responsibility is to close stream upon consumption)
     *
     * @throws FileNotFoundException
     */
    InputStream download(String bundleName, String handlingNode) throws FileNotFoundException;

    /**
     * @return support bundle output directory
     */
    File getOutputDirectory();
}

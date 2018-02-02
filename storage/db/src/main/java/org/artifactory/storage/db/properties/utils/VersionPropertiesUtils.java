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

package org.artifactory.storage.db.properties.utils;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ArtifactoryRunningMode;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.storage.db.properties.DbVersionInfo;
import org.artifactory.storage.db.version.ArtifactoryDBVersion;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ArtifactoryVersionReader;
import org.artifactory.version.CompoundVersionDetails;

/**
 * Date: 8/27/13 9:29 PM
 *
 * @author freds
 */
public abstract class VersionPropertiesUtils {

    /**
     * Compare the version of Artifactory running with the DB version, and return:<ul>
     * <li><b>true</b> : if all is OK (server can run) and no upgrades to do.</li>
     * <li><b>false</b> : if an upgrades needs to be applied.</li>
     * <li><b>IllegalStateException</b> : if the version are incompatible.</li>
     * </ul>
     *
     * @param dbVersionDetails The version found in Db props
     * @param versionDetails   The running version of Artifactory
     * @return true all ok, false upgrade needed
     * @throws java.lang.IllegalStateException
     *          If compatibility cannot be done
     */
    public static boolean isDbPropertiesVersionCompatible(
            CompoundVersionDetails dbVersionDetails, CompoundVersionDetails versionDetails) {
        // TODO: [by fsi] compare version type, impossible to run old version on new or vice-versa
        // TODO: [by fsi] Info should come from ArtifactoryDBVersion
        // For now just compare the version and then release number
        if (dbVersionDetails.getVersion().before(versionDetails.getVersion())) {
            // Need upgrade
            return false;
        }
        if (dbVersionDetails.getVersion().after(versionDetails.getVersion())) {
            // Cannot upgrade running below DB
            return true;
        }
        // If DB above all good
        return dbVersionDetails.getRevisionInt() >= versionDetails.getRevisionInt();
    }

    public static CompoundVersionDetails getBeforeAutoDbConvertVersionDetails() {
        // Before auto convert implemented
        ArtifactoryVersion firstDbVersion = ArtifactoryDBVersion.v100.getComparator().getFrom();
        return new CompoundVersionDetails(firstDbVersion, firstDbVersion.getValue(),
                "" + firstDbVersion.getRevision(), "FROM_DB", 0L);
    }

    public static CompoundVersionDetails getDbCompoundVersionDetails(DbVersionInfo versionInfo) {
        if (versionInfo == null) {
            // First version with props and no value
            ArtifactoryVersion firstPropsDbVersion = ArtifactoryDBVersion.v101.getComparator().getFrom();
            return new CompoundVersionDetails(firstPropsDbVersion, firstPropsDbVersion.getValue(),
                    "" + firstPropsDbVersion.getRevision(), "FROM_DB", 0L);
        }
        return ArtifactoryVersionReader.getCompoundVersionDetails(
                versionInfo.getArtifactoryVersion(),
                getRevisionStringFromInt(versionInfo.getArtifactoryRevision()),
                "" + versionInfo.getArtifactoryRelease());
    }

    public static DbVersionInfo createDbPropertiesFromVersion(CompoundVersionDetails versionDetails) {
        long installTime = System.currentTimeMillis();
        return new DbVersionInfo(installTime,
                versionDetails.getVersionName(),
                versionDetails.getRevisionInt(),
                versionDetails.getTimestamp()
        );
    }

    private static ArtifactoryRunningMode getCurrentArtifactoryMode() {
        ArtifactoryContext context = ContextHelper.get();
        if (context == null) {
            return ArtifactoryRunningMode.OSS;
        }
        AddonsManager addonsManager = context.beanForType(AddonsManager.class);
        return addonsManager.getArtifactoryRunningMode();
    }

    private static String getRevisionStringFromInt(int rev) {
        if (rev <= 0 || rev == Integer.MAX_VALUE) {
            return "" + Integer.MAX_VALUE;
        }
        return "" + rev;
    }
}

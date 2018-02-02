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

package org.artifactory.version;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ConstantValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.artifactory.version.ArtifactoryVersion.isCurrentVersion;

/**
 * Returns ArtifactoryVersion object from a properties stream/file.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryVersionReader {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryVersionReader.class);

    // First release of Artifactory
    public static long UNFILTERED_TIMESTAMP = 1167040800000L;

    public static class VersionPropertiesContent {
        String versionString;
        String revisionString;
        String buildNumberString;
        String timestampString;

        public VersionPropertiesContent() {
        }

        public VersionPropertiesContent(String version, String revision, String buildNumber, String timestamp) {
            this.versionString = version;
            this.revisionString = revision;
            this.buildNumberString = buildNumber;
            this.timestampString = timestamp;
        }
    }

    public static CompoundVersionDetails readFromFileAndFindVersion(File propertiesFile) {
        if (propertiesFile == null) {
            throw new IllegalArgumentException("Null properties file is not allowed");
        }
        try {
            return readAndFindVersion(new FileInputStream(propertiesFile), propertiesFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Properties file " + propertiesFile.getName() + " doesn't exist");
        }
    }

    public static VersionPropertiesContent readPropsContent(InputStream inputStream, String sourceName) {
        VersionPropertiesContent res = new VersionPropertiesContent();
        if (inputStream == null) {
            throw new IllegalArgumentException("Artifactory properties input stream cannot be null");
        }
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read version details from '" + sourceName + "'", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        String versionPropName = ConstantValues.artifactoryVersion.getPropertyName();
        res.versionString = props.getProperty(versionPropName);
        if (StringUtils.isBlank(res.versionString)) {
            throw new IllegalArgumentException("Version source '" + sourceName +
                    "' does not have the mandatory property '" + versionPropName + "'");
        }
        res.revisionString = props.getProperty(ConstantValues.artifactoryRevision.getPropertyName());
        res.timestampString = props.getProperty(ConstantValues.artifactoryTimestamp.getPropertyName());
        res.buildNumberString = props.getProperty(ConstantValues.artifactoryBuildNumber.getPropertyName());

        return res;
    }

    public static CompoundVersionDetails readAndFindVersion(InputStream inputStream, String sourceName) {
        return getCompoundVersionDetails(readPropsContent(inputStream, sourceName));
    }

    public static CompoundVersionDetails getCompoundVersionDetails(String version, String revision,
            String timestamp) {
        return getCompoundVersionDetails(new VersionPropertiesContent(version, revision, "UNDEFINED", timestamp));
    }

    public static CompoundVersionDetails getCompoundVersionDetails(VersionPropertiesContent versionProps) {
        if (StringUtils.isBlank(versionProps.versionString)) {
            throw new IllegalArgumentException("Version value cannot be empty");
        }
        ArtifactoryVersion matchedVersion = null;
        // TODO: [by fsi] STRK-15 All development version are current in an hardcoded way!
        if (isCurrentVersion(versionProps.versionString, versionProps.revisionString)) {
            matchedVersion = ArtifactoryVersion.getCurrent();
        }
        long timestamp;
        try {
            if ("${timestamp}".equals(versionProps.timestampString) || "LOCAL".equals(versionProps.timestampString)) {
                // In dev mode
                timestamp = UNFILTERED_TIMESTAMP;
            } else {
                timestamp = Long.parseLong(versionProps.timestampString);
            }
        } catch (Exception e) {
            log.warn("Could not parse timestamp value "+versionProps.timestampString);
            timestamp = 0;
        }

        if (matchedVersion == null) {
            matchedVersion = findByVersionString(versionProps.versionString, versionProps.revisionString);
            if (matchedVersion != null) {
                log.warn("Closest matched version: {}", matchedVersion.getValue());
            }
        }
        if (matchedVersion == null) {
            matchedVersion = findClosestMatch(versionProps.versionString, versionProps.revisionString);
            if (matchedVersion != null) {
                log.warn("Closest matched version: {}", matchedVersion.getValue());
            }
        }

        if (matchedVersion == null) {
            throw new IllegalStateException("No version declared is higher than " + versionProps.versionString +
                    " : " + versionProps.revisionString);
        }

        return new CompoundVersionDetails(matchedVersion, versionProps.versionString, versionProps.revisionString,
                versionProps.buildNumberString, timestamp);
    }

    private static ArtifactoryVersion findByVersionString(String versionString, String revisionString) {
        int artifactoryRevision = Integer.parseInt(revisionString);
        String mavenVersion = ArtifactoryVersion.convertToMavenVersion(versionString);
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.getValue().equals(mavenVersion)) {
                if (artifactoryRevision != version.getRevision()) {
                    log.warn("Version found is " + version + " but the revision " +
                            artifactoryRevision + " is not the one supported!\n" +
                            "Reading the folder may work with this version.\n" +
                            "For Information: Using the Command Line Tool is preferable in this case.");
                }
                return version;
            }
        }
        return null;
    }

    private static ArtifactoryVersion findClosestMatch(String versionString, String revisionString) {
        int artifactoryRevision = Integer.parseInt(revisionString);
        log.warn("Version " + versionString + " is not an official release version. " +
                "The closest released revision to " + artifactoryRevision + " will be used to determine the current " +
                "version.\nWarning: This version is unsupported! Reading backup data may not work!\n");
        ArtifactoryVersion[] values = ArtifactoryVersion.values();
        for (int i = values.length - 1; i >= 0; i--) {
            ArtifactoryVersion version = values[i];
            if (artifactoryRevision >= version.getRevision()) {
                return version;
            }
        }
        return null;
    }
}

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

package org.artifactory.logging.version;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.logging.version.v1.LogbackConfigSwapper;
import org.artifactory.logging.version.v3.LogbackJFrogInfoConverter;
import org.artifactory.logging.version.v5.LogbackRemoveSupportLogConverter;
import org.artifactory.logging.version.v7.LogbackAddAccessServerLogsConverter;
import org.artifactory.logging.version.v8.LogbackBackTracePatternLayoutConverter;
import org.artifactory.logging.version.v9.LogbackAddMigrationLogsConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.XmlConverterUtils;
import org.artifactory.version.converter.XmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps track of the logging configuration versions
 *
 * @author Noam Y. Tenne
 */
public enum LoggingVersion implements SubConfigElementVersion {
    v1(ArtifactoryVersion.v122rc0, ArtifactoryVersion.v304, new LogbackConfigSwapper()),
    v2(ArtifactoryVersion.v310, ArtifactoryVersion.v331, null),
    v3(ArtifactoryVersion.v340, ArtifactoryVersion.v421, new LogbackJFrogInfoConverter()),
    v4(ArtifactoryVersion.v422, ArtifactoryVersion.v440, new LogbackConfigSwapper()),
    /**
     * Last commit really messed up the versions here... v5 already existed when he committed v4.
     * v4 was originally RTFACT-4550 --> art v2.5.0 and v5 was originally RTFACT-6766 --> art v3.3.0
     * SO to accommodate the last change up to Artifactory v4.4.0 the logback swapper conversion will happen,
     * v5 is now a dummy version that does not change logback up to v4.8.2
     * v6 is the current version which triggers remove support appender conversion
     */
    v5(ArtifactoryVersion.v441, ArtifactoryVersion.v484, new LogbackRemoveSupportLogConverter()),
    v6(ArtifactoryVersion.v490, ArtifactoryVersion.v4161),
    v7(ArtifactoryVersion.v500beta1, ArtifactoryVersion.v534, new LogbackAddAccessServerLogsConverter()),
    v8(ArtifactoryVersion.v540m001, ArtifactoryVersion.v548, new LogbackBackTracePatternLayoutConverter()),
    v9(ArtifactoryVersion.v550m001, ArtifactoryVersion.getCurrent(), new LogbackAddMigrationLogsConverter());

    private static final Logger log = LoggerFactory.getLogger(LoggingVersion.class);

    private final VersionComparator comparator;
    private XmlConverter[] xmlConverters;

    /**
     * Main constructor
     *
     * @param from          Start version
     * @param until         End version
     * @param xmlConverters XML converters required for the specified range
     */
    LoggingVersion(ArtifactoryVersion from, ArtifactoryVersion until, XmlConverter... xmlConverters) {
        this.xmlConverters = xmlConverters;
        this.comparator = new VersionComparator(from, until);
        ArtifactoryVersion.addSubConfigElementVersion(this, comparator);
    }

    /**
     * Run the needed conversions
     *
     * @param srcEtcDir the directory in which resides the logback file to convert
     */
    public void convert(File srcEtcDir, File targetEtcDir) throws IOException {
        // First create the list of converters to apply
        List<XmlConverter> converters = new ArrayList<>();

        // All converters of versions above me needs to be executed in sequence
        LoggingVersion[] versions = LoggingVersion.values();
        for (LoggingVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.xmlConverters != null) {
                converters.addAll(Arrays.stream(version.xmlConverters).collect(Collectors.toList()));
            }
        }

        if (!converters.isEmpty()) {
            File logbackConfigFile = new File(srcEtcDir, ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
            try {
                String result =
                        XmlConverterUtils.convert(converters, FileUtils.readFileToString(logbackConfigFile, "utf-8"));
                backupAndSaveLogback(result, targetEtcDir);
            } catch (IOException e) {
                log.error("Error occurred while converting logback config for conversion: {}.", e.getMessage());
                log.debug("Error occurred while converting logback config for conversion", e);
                throw e;
            }
        }
    }

    @Override
    public VersionComparator getComparator() {
        return comparator;
    }

    /**
     * Creates a backup of the existing logback configuration file and proceeds to save post-conversion content
     *
     * @param result Conversion result
     * @param etcDir directory to which to save the conversion result
     */
    public void backupAndSaveLogback(String result, File etcDir) throws IOException {
        File logbackConfigFile = new File(etcDir, ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
        if (logbackConfigFile.exists()) {
            File originalBackup = new File(etcDir, "logback.original.xml");
            if (originalBackup.exists()) {
                FileUtils.deleteQuietly(originalBackup);
            }
            FileUtils.moveFile(logbackConfigFile, originalBackup);
        }

        FileUtils.writeStringToFile(logbackConfigFile, result, "utf-8");
    }

    public static void convert(ArtifactoryVersion from, ArtifactoryVersion target, File path)
            throws IOException {
        boolean foundConversion = false;
        // All converters of versions above me needs to be executed in sequence
        LoggingVersion[] versions = LoggingVersion.values();
        for (LoggingVersion version : versions) {
            if (version.comparator.isAfter(from) && !version.comparator.supports(from)) {
                version.convert(path, path);
            }
        }
        // Write to log only if conversion has been executed
        if (foundConversion) {
            log.info("Ending database conversion from " + from + " to " + target);
        }
    }
}
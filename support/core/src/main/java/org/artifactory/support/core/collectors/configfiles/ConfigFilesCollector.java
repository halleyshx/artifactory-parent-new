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

package org.artifactory.support.core.collectors.configfiles;

import com.google.common.collect.Lists;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.core.collectors.AbstractSpecificContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Config files collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class ConfigFilesCollector extends AbstractSpecificContentCollector<ConfigFilesConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(ConfigFilesCollector.class);
    private final List<File> configFiles = getConfigFiles();
    private static final String[] STRINGS_TO_SCRUB = {"password=", "<password>", "<refreshToken>", "<secret>",
            "<managerPassword>", "<passphrase>", "<gpgPassPhrase>", "<keyStorePassword>"};

    public ConfigFilesCollector() {
        super("config-files");
    }

    /**
     * @return config files
     */
    private static List<File> getConfigFiles() {
        List<File> configFiles = Lists.newArrayList();

        configFiles.add(ArtifactoryHome.get().getArtifactoryConfigBootstrapFile());
        configFiles.add(ArtifactoryHome.get().getArtifactoryConfigFile());
        configFiles.add(ArtifactoryHome.get().getArtifactoryConfigImportFile());
        configFiles.add(ArtifactoryHome.get().getArtifactoryConfigLatestFile());
        configFiles.add(ArtifactoryHome.get().getArtifactoryConfigNewBootstrapFile());
        configFiles.add(ArtifactoryHome.get().getLogbackConfig());
        configFiles.add(ArtifactoryHome.get().getMimeTypesFile());
        configFiles.add(ArtifactoryHome.get().getDBPropertiesFile());

        return configFiles;
    }

    /**
     * Collects SystemLogs
     *
     * @param configuration {@link org.artifactory.support.config.systemlogs.SystemLogsConfiguration}
     * @param tmpDir        output directory for produced content
     * @return operation result
     */
    @Override
    protected boolean doCollect(ConfigFilesConfiguration configuration, File tmpDir) {
        if (configFiles != null && configFiles.size() > 0) {
            try {
                configFiles.stream()
                        .filter(f -> Files.exists(f.toPath()))
                        .filter(f -> Files.isRegularFile(f.toPath()))
                        .forEach(f -> {
                            if (configuration.isHideUserDetails()) {
                                scrubUserDataToTempDir(f.toPath(), tmpDir);
                            } else {
                                copyToTempDir(f.toPath(), tmpDir);
                            }
                        });
                getLog().info("Collection of " + getContentName() + " was successfully accomplished");
                return true;
            } catch (Exception e) {
                return failure(e);
            }
        } else {
            getLog().debug("No items to work on");
            return false;
        }
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws org.artifactory.support.core.exceptions.BundleConfigurationException if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(ConfigFilesConfiguration configuration)
            throws BundleConfigurationException {
        ;
    }

    private void scrubUserDataToTempDir(Path file, File tmpDir) {
        File outputFile = new File(tmpDir.getPath() + File.separator + file.getFileName());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                boolean writeLine = true;
                for (String password : STRINGS_TO_SCRUB) {
                    if (line.contains(password)) {
                        writeLine = false;
                    }
                }
                if (writeLine) {
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

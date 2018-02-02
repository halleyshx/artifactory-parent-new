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

package org.artifactory.support.core.collectors.config;

import com.google.common.base.Strings;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.core.collectors.AbstractGenericContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.utils.StringBuilderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * Config descriptor collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class ConfigDescriptorCollector extends AbstractGenericContentCollector<ConfigDescriptorConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(ConfigDescriptorCollector.class);
    private static final String[] TO_SCRUB = {
            "\\s*+<password>.+</password>",
            "\\s*+<keyStorePassword>.+</keyStorePassword>",
            "\\s*+<passphrase>.+</passphrase>",
            "\\s*+<gpgPassPhrase>.+</gpgPassPhrase>",
            "\\s*+<refreshToken>.+</refreshToken>",
            "\\s*+<secret>.+</secret>",
            "\\s*+<managerPassword>.+</managerPassword>"};

    @Autowired
    private CentralConfigService centralConfigService;

    public ConfigDescriptorCollector() {
        super("config-descriptor");
    }

    /**
     * Produces content and returns it wrapped with {@link StringBuilderWrapper}
     *
     * @param configuration the runtime configuration
     * @return {@link StringBuilderWrapper}
     */
    @Override
    protected StringBuilderWrapper doProduceContent(ConfigDescriptorConfiguration configuration)
            throws InstantiationException,
            IllegalAccessException, IOException {
        String centralConfigDescriptor = getDescriptorData(configuration);

        if (!Strings.isNullOrEmpty(centralConfigDescriptor)) {
            return new StringBuilderWrapper(centralConfigDescriptor);
        }
        return failure();
    }

    /**
     * Performs filtering on returned data
     *
     * @param configuration {@link ConfigDescriptorConfiguration}
     * @return {@link CentralConfigDescriptor}
     */
    private String getDescriptorData(ConfigDescriptorConfiguration configuration) {
        String configDescriptor = centralConfigService.getConfigXml();

        if (!configuration.isHideUserDetails()) {
            return configDescriptor;
        }
        Arrays.stream(TO_SCRUB).forEach(scrub -> configDescriptor.replaceAll(scrub, ""));
        return configDescriptor;
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
    protected void doEnsureConfiguration(ConfigDescriptorConfiguration configuration)
            throws BundleConfigurationException {
    }
}

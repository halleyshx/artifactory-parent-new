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

package org.artifactory.support.core.collectors.system;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.info.InfoWriter;
import org.artifactory.state.model.StateInitializer;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.core.collectors.AbstractGenericContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.utils.StringBuilderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * System info collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class SystemInfoCollector extends AbstractGenericContentCollector<SystemInfoConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(SystemInfoCollector.class);

    public SystemInfoCollector() {
        super("system-info");
    }

    /**
     * Produces content and returns it wrapped with {@link StringBuilderWrapper}
     *
     * @param configuration the runtime configuration
     *
     * @return {@link StringBuilderWrapper}
     *
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    protected StringBuilderWrapper doProduceContent(SystemInfoConfiguration configuration)
            throws InstantiationException,
            IllegalAccessException, IOException {
        String info = InfoWriter.getInfo();
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        String pluginsStatus = artifactoryContext.beanForType(AddonsManager.class).addonByType(PluginsAddon.class)
                .getPluginsInfoSupportBundleDump();
        StateInitializer stateInitializer = artifactoryContext.beanForType(StateInitializer.class);
        String stateInfo = stateInitializer.getSupportBundleDump();
        if (!Strings.isNullOrEmpty(info)) {
            StringBuilderWrapper builderWrapper = new StringBuilderWrapper(info);
            if (StringUtils.isNotBlank(pluginsStatus)) {
                builderWrapper.newLine();
                builderWrapper.append(pluginsStatus);
            }
            builderWrapper.newLine();
            builderWrapper.append(stateInfo);
            return builderWrapper;
        } else {
            getLog().debug("No content was fetched from InfoWriter");
        }
        return failure();
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws org.artifactory.support.core.exceptions.BundleConfigurationException
     *         if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(SystemInfoConfiguration configuration)
            throws BundleConfigurationException {
        ;
    }
}

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

package org.artifactory.converter.helpers;

import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.config.db.ArtifactoryCommonDbPropertiesService;
import org.artifactory.converter.ConverterManager;
import org.artifactory.converter.ConvertersManagerImpl;
import org.artifactory.converter.VersionProviderImpl;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.spring.SpringConfigPaths;
import org.artifactory.state.model.ArtifactoryStateManager;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.VersionProvider;
import org.jfrog.config.ConfigurationManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
public class MockArtifactoryContext implements ArtifactoryContext {

    private final MockDbPropertiesService mockDbPropertiesService;
    private final MockArtifactoryStateManager mockArtifactoryStateManager;
    private final MockArtifactoryServersCommonService mockArtifactoryServersCommonService;
    private final AddonsManager addonsManager;
    private ConvertersManagerImpl convertersManager;
    private VersionProviderImpl versionProvider;
    private ConfigurationManager configurationManager;

    public MockArtifactoryContext(ArtifactoryVersion version, long release, ConvertersManagerImpl convertersManager,
            VersionProviderImpl versionProvider, ConfigurationManager configurationManager, final boolean validLicense) {
        this.convertersManager = convertersManager;
        this.versionProvider = versionProvider;
        this.configurationManager = configurationManager;
        mockDbPropertiesService = new MockDbPropertiesService(version, release);
        mockArtifactoryStateManager = new MockArtifactoryStateManager();
        mockArtifactoryServersCommonService = new MockArtifactoryServersCommonService(version);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class[] interfaces = {AddonsManager.class};
        addonsManager = (AddonsManager) Proxy.newProxyInstance(contextClassLoader, interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("lockdown".equals(method.getName())) {
                    return !validLicense;
                }
                return null;
            }
        });
    }

    @Override
    public <T> T beanForType(String name, Class<T> type) {
        return null;
    }

    @Override
    public CentralConfigService getCentralConfig() {
        return null;
    }

    @Override
    public <T> T beanForType(Class<T> type) {
        if (type.equals(ArtifactoryCommonDbPropertiesService.class)) {
            return (T) mockDbPropertiesService;
        }
        if (type.equals(ArtifactoryStateManager.class)) {
            return (T) mockArtifactoryStateManager;
        }
        if (type.equals(ArtifactoryServersCommonService.class)) {
            return (T) mockArtifactoryServersCommonService;
        }
        if (type.equals(AddonsManager.class)) {
            return (T) addonsManager;
        }

        return null;
    }

    @Override
    public <T> Map<String, T> beansForType(Class<T> type) {
        return null;
    }

    @Override
    public Object getBean(String name) {
        return null;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return null;
    }

    @Override
    public AuthorizationService getAuthorizationService() {
        return null;
    }

    @Override
    public long getUptime() {
        return 0;
    }

    @Override
    public ArtifactoryHome getArtifactoryHome() {
        return null;
    }

    @Override
    public String getContextId() {
        return null;
    }

    @Override
    public SpringConfigPaths getConfigPaths() {
        return null;
    }

    @Override
    public String getServerId() {
        return "test";
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public void setOffline() {
    }

    @Override
    public ConfigurationManager getConfigurationManager() {
        return null;
    }

    @Override
    public ConverterManager getConverterManager() {
        return convertersManager;
    }

    @Override
    public VersionProvider getVersionProvider() {
        return versionProvider;
    }

    @Override
    public void destroy() {
        // NOOP
    }

    @Override
    public void exportTo(ExportSettings settings) {
    }

    @Override
    public void importFrom(ImportSettings settings) {
    }

    public MockDbPropertiesService getMockDbPropertiesService() {
        return mockDbPropertiesService;
    }

    public MockArtifactoryStateManager getMockArtifactoryStateManager() {
        return mockArtifactoryStateManager;
    }
}
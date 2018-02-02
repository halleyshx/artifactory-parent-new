package org.artifactory.configuration.helper;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converter.ConverterManager;
import org.artifactory.converter.ConvertersManagerImpl;
import org.artifactory.converter.VersionProviderImpl;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.spring.SpringConfigPaths;
import org.artifactory.version.VersionProvider;
import org.artifactory.webapp.servlet.BasicConfigManagers;
import org.jfrog.config.ConfigurationManager;

import java.util.Map;

/**
 * @author gidis
 */
public class TestingArtifactoryContext implements ArtifactoryContext {
    private ConverterManager convertersManager;
    private VersionProvider versionProvider;
    private ConfigurationManager configurationManager;

    public TestingArtifactoryContext(BasicConfigManagers basicConfigManagers, ConfigurationManager configurationManager){
        this.convertersManager = basicConfigManagers.convertersManager;
        this.versionProvider = basicConfigManagers.versionProvider;
        this.configurationManager = configurationManager;
    }

    @Override
    public void exportTo(ExportSettings settings) {

    }

    @Override
    public void importFrom(ImportSettings settings) {

    }

    @Override
    public CentralConfigService getCentralConfig() {
        return null;
    }

    @Override
    public <T> T beanForType(Class<T> type) {
        return null;
    }

    @Override
    public <T> T beanForType(String name, Class<T> type) {
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
        return null;
    }

    @Override
    public boolean isReady() {
        return false;
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
        return null;
    }

    @Override
    public VersionProvider getVersionProvider() {
        return null;
    }

    @Override
    public void destroy() {

    }
}

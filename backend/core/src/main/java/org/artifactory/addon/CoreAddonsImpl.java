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

package org.artifactory.addon;

import com.google.common.collect.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.sshd.server.Command;
import org.artifactory.addon.bower.BowerAddon;
import org.artifactory.addon.bower.BowerMetadataInfo;
import org.artifactory.addon.build.ArtifactBuildAddon;
import org.artifactory.addon.chef.ChefAddon;
import org.artifactory.addon.chef.ChefCookbookInfo;
import org.artifactory.addon.cocoapods.CocoaPodsAddon;
import org.artifactory.addon.composer.ComposerAddon;
import org.artifactory.addon.composer.ComposerMetadataInfo;
import org.artifactory.addon.conan.ConanAddon;
import org.artifactory.addon.conan.info.ConanPackageInfo;
import org.artifactory.addon.conan.info.ConanRecipeInfo;
import org.artifactory.addon.debian.DebianAddon;
import org.artifactory.addon.debian.DebianMetadataInfo;
import org.artifactory.addon.debian.index.DebianCalculationEvent;
import org.artifactory.addon.distribution.DistributionAddon;
import org.artifactory.addon.docker.DockerAddon;
import org.artifactory.addon.docker.DockerV2InfoModel;
import org.artifactory.addon.docker.rest.DockerTokenCacheKey;
import org.artifactory.addon.filteredresources.FilteredResourcesAddon;
import org.artifactory.addon.gems.ArtifactGemsInfo;
import org.artifactory.addon.gems.GemsAddon;
import org.artifactory.addon.gitlfs.GitLfsAddon;
import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;
import org.artifactory.addon.ha.propagation.uideploy.UIDeployPropagationResult;
import org.artifactory.addon.ha.workitem.HaMessageWorkItem;
import org.artifactory.addon.helm.HelmAddon;
import org.artifactory.addon.helm.HelmMetadataInfo;
import org.artifactory.addon.ldapgroup.LdapUserGroup;
import org.artifactory.addon.ldapgroup.LdapUserGroupAddon;
import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.addon.npm.NpmAddon;
import org.artifactory.addon.npm.NpmMetadataInfo;
import org.artifactory.addon.nuget.UiNuGetAddon;
import org.artifactory.addon.oauth.OAuthSsoAddon;
import org.artifactory.addon.opkg.OpkgAddon;
import org.artifactory.addon.opkg.OpkgCalculationEvent;
import org.artifactory.addon.properties.ArtifactPropertiesAddon;
import org.artifactory.addon.puppet.PuppetAddon;
import org.artifactory.addon.puppet.PuppetInfo;
import org.artifactory.addon.pypi.PypiAddon;
import org.artifactory.addon.pypi.PypiPkgMetadata;
import org.artifactory.addon.release.bundle.ReleaseBundleAddon;
import org.artifactory.addon.replication.LocalReplicationSettings;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.addon.replication.event.ReplicationChannel;
import org.artifactory.addon.replication.event.ReplicationEventQueueWorkItem;
import org.artifactory.addon.replication.event.ReplicationOwnerModel;
import org.artifactory.addon.security.JvmConflictGuardProvider;
import org.artifactory.addon.smartrepo.SmartRepoAddon;
import org.artifactory.addon.sso.HttpSsoAddon;
import org.artifactory.addon.sso.crowd.CrowdAddon;
import org.artifactory.addon.sso.crowd.CrowdExtGroup;
import org.artifactory.addon.sso.saml.SamlSsoAddon;
import org.artifactory.addon.support.SupportAddon;
import org.artifactory.addon.watch.ArtifactWatchAddon;
import org.artifactory.addon.webstart.ArtifactWebstartAddon;
import org.artifactory.addon.xray.ArtifactXrayInfo;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.addon.xray.XrayRepo;
import org.artifactory.addon.xray.XrayScanBuild;
import org.artifactory.addon.yum.ArtifactRpmMetadata;
import org.artifactory.addon.yum.YumAddon;
import org.artifactory.api.bintray.distribution.FileSpec;
import org.artifactory.api.bintray.docker.BintrayDockerPushRequest;
import org.artifactory.api.build.GeneralBuild;
import org.artifactory.api.build.ModuleArtifact;
import org.artifactory.api.build.ModuleDependency;
import org.artifactory.api.build.PublishedModule;
import org.artifactory.api.build.diff.BuildsDiffBaseFileModel;
import org.artifactory.api.build.diff.BuildsDiffPropertyModel;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.license.LicenseInfo;
import org.artifactory.api.license.LicensesInfo;
import org.artifactory.api.license.ModuleLicenseModel;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.rest.build.diff.BuildsDiff;
import org.artifactory.api.rest.release.ReleaseBundleRequest;
import org.artifactory.api.rest.release.ReleaseBundleResult;
import org.artifactory.api.rest.replication.ReplicationStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.ArtifactoryBuildArtifact;
import org.artifactory.build.BuildId;
import org.artifactory.build.BuildRun;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.group.LdapGroupPopulatorStrategies;
import org.artifactory.descriptor.security.ldap.group.LdapGroupSetting;
import org.artifactory.descriptor.security.sso.CrowdSettings;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.*;
import org.artifactory.ldap.LdapUserSearchesHelper;
import org.artifactory.md.Properties;
import org.artifactory.nuget.NuMetaData;
import org.artifactory.repo.*;
import org.artifactory.repo.cache.expirable.CacheExpiryStrategyImpl;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.service.mover.MoverConfig;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.schedule.Task;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.props.auth.SshTokenManager;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.storage.fs.lock.FsItemsVault;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.security.util.Pair;
import org.jfrog.storage.common.ConflictGuard;
import org.jfrog.storage.common.ConflictsGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.OperationNotSupportedException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the core-related addon factories.
 *
 * @author Yossi Shaul
 */
@Component
public class CoreAddonsImpl implements WebstartAddon, LdapGroupAddon, LicensesAddon, PropertiesAddon, LayoutsCoreAddon,
        FilteredResourcesAddon, ReplicationAddon, YumAddon, NuGetAddon, RestCoreAddon, CrowdAddon,
        GemsAddon, HaAddon, NpmAddon, BowerAddon, DebianAddon, OpkgAddon, PypiAddon, PuppetAddon, DockerAddon,
        VagrantAddon,
        ArtifactWatchAddon, ArtifactBuildAddon, UiNuGetAddon, LdapUserGroupAddon, GitLfsAddon, CocoaPodsAddon,
        ArtifactWebstartAddon, SamlSsoAddon, OAuthSsoAddon, HttpSsoAddon, SmartRepoAddon, ArtifactPropertiesAddon,
        SupportAddon, XrayAddon, ComposerAddon, ConanAddon, ChefAddon, ReleaseBundleAddon, HelmAddon, DistributionAddon, Addon {

    private static final Logger log = LoggerFactory.getLogger(CoreAddonsImpl.class);
    private JvmConflictGuardProvider jvmConflictGuardProvider;

    public CoreAddonsImpl() {
        log.debug("Initializing JVM locking provider");
    }

    private JvmConflictGuardProvider getConflictGuardProvider() {
        if (jvmConflictGuardProvider == null) {
            synchronized (this) {
                if (jvmConflictGuardProvider == null) {
                    jvmConflictGuardProvider = new JvmConflictGuardProvider();
                }
            }
        }
        return jvmConflictGuardProvider;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public VirtualRepo createVirtualRepo(InternalRepositoryService repoService, VirtualRepoDescriptor descriptor) {
        return new VirtualRepo(descriptor, repoService);
    }

    @Override
    public void importKeyStore(ImportSettings settings) {
        // do nothing
    }

    @Override
    public void exportKeyStore(ExportSettings exportSettings) {
        // do nothing
    }

    @Override
    public void addExternalGroups(String userName, Set<UserGroupInfo> groups) {
        // nop
    }

    @Override
    public Set<CrowdExtGroup> findCrowdExtGroups(String username, CrowdSettings currentCrowdSettings) {
        return null;
    }

    @Override
    public void testCrowdConnection(CrowdSettings crowdSettings) throws Exception {
    }

    @Override
    public void logOffCrowd(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public void populateGroups(DirContextOperations dirContextOperations, MutableUserInfo userInfo) {
        // do nothing
    }

    @Override
    public void populateGroups(String dn, MutableUserInfo info) {
        // do nothing
    }

    @Override
    public List<LdapSetting> getEnabledLdapSettings() {
        CentralConfigDescriptor descriptor = ContextHelper.get().beanForType(
                CentralConfigService.class).getDescriptor();
        List<LdapSetting> enabledLdapSettings = descriptor.getSecurity().getEnabledLdapSettings();
        if (enabledLdapSettings != null && !enabledLdapSettings.isEmpty()) {
            return Lists.newArrayList(enabledLdapSettings.get(0));
        }
        return Lists.newArrayList();
    }

    @Override
    public List<LdapUserSearch> getLdapUserSearches(ContextSource ctx, LdapSetting settings, boolean aol) {
        return LdapUserSearchesHelper.getLdapUserSearches(ctx, settings, aol);
    }

    @Override
    public void performOnBuildArtifacts(Build build) {
        // NOP
    }

    @Override
    public void addPropertySetToRepository(RealRepoDescriptor descriptor) {
        // NOP
    }

    @Override
    public void importLicenses(ImportSettings settings) {
        // NOP
    }

    @Override
    public void exportLicenses(ExportSettings exportSettings) {
        // nop
    }

    @Override
    public List<ModuleLicenseModel> findLicensesInRepos(Set<String> repoKeys, LicenseStatus status) {
        return Lists.newArrayList();
    }

    @Override
    public LicensesInfo getArtifactsLicensesInfo() {
        return null;
    }

    @Override
    public String writeLicenseXML(LicensesInfo licensesInfo) {
        return null;
    }

    @Override
    public void addLicenseInfo(LicenseInfo licensesInfo) {

    }

    @Override
    public void updateLicenseInfo(LicenseInfo licensesInfo) {
    }

    @Override
    public void deleteLicenseInfo(LicenseInfo licensesInfo) {

    }

    @Override
    public LicenseInfo getLicenseByName(String licenseName) {
        return new LicenseInfo();
    }

    @Override
    public void reloadLicensesCache() {
    }

    @Override
    public Multimap<RepoPath, ModuleLicenseModel> populateLicenseInfoSynchronously(Build build, boolean autoDiscover) {
        return HashMultimap.create();
    }

    @Override
    public String generateLicenseCsv(Collection<ModuleLicenseModel> models) {
        return null;
    }

    @Override
    public boolean setLicensePropsOnPath(RepoPath path, Set<LicenseInfo> licenses) {
        return false;
    }

    @Override
    public void setLicensePropsOnPath(RepoPath path, String... licenseNames) {
    }

    @Override
    public Set<LicenseInfo> scanPathForLicenses(RepoPath path) {
        return Sets.newHashSet();
    }

    @Override
    public Set<LicenseInfo> getPathLicensesByProps(RepoPath path) {
        return null;
    }

    @Override
    public Properties getProperties(RepoPath repoPath) {
        return (Properties) InfoFactoryHolder.get().createProperties();
    }

    @Override
    public Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths) {
        return Maps.newHashMap();
    }

    @Override
    public void deleteProperty(RepoPath repoPath, String property) {
        // nop
    }

    @Override
    public void addProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values) {
        //nop
    }

    @Override
    public void setProperties(RepoPath repoPath, Properties properties) {
        //nop
    }

    @Override
    public RepoResource assembleDynamicMetadata(InternalRequestContext context, RepoPath metadataRepoPath) {
        return new FileResource(ContextHelper.get().getRepositoryService().getFileInfo(metadataRepoPath));
    }

    @Override
    public void updateRemoteProperties(Repo repo, RepoPath repoPath) {
        // nop
    }

    @Override
    public boolean isFilteredResourceFile(RepoPath repoPath) {
        return false;
    }

    @Override
    public boolean isFilteredResourceFile(RepoPath repoPath, Properties props) {
        return false;
    }

    @Override
    public RepoResource getFilteredResource(Request request, FileInfo fileInfo, InputStream fileInputStream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Creation of a filtered resource requires the Properties add-on.", HttpStatus.SC_FORBIDDEN);
    }

    @Override
    public RepoResource getZipResource(Request request, FileInfo fileInfo, InputStream stream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Direct resource download from zip requires the Filtered resources add-on.", HttpStatus.SC_FORBIDDEN);
    }

    @Override
    public ResourceStreamHandle getZipResourceHandle(ZipEntryRepoResource resource, InputStream stream) {
        throw new UnsupportedOperationException(
                "Direct resource download from zip requires the Filtered resources add-on.");
    }

    @Override
    public String getGeneratedSettingsUsernameTemplate() {
        return "${security.getCurrentUsername()}";
    }

    @Override
    public String getGeneratedSettingsUserCredentialsTemplate(boolean escape) {
        AuthorizationService authorizationService = ContextHelper.get().getAuthorizationService();

        if (authorizationService.isAnonymous() || authorizationService.isTransientUser()) {
            return "";
        }

        StringBuilder credentialsTemplateBuilder = new StringBuilder("${security.get");
        if (escape) {
            credentialsTemplateBuilder.append("Escaped");
        }
        return credentialsTemplateBuilder.append("EncryptedPassword()!\"*** Insert encrypted password here ***\"}")
                .toString();
    }

    @Override
    public String filterResource(Request request, Properties contextProperties, Reader reader) throws Exception {
        try {
            return IOUtils.toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public String filterResource(Reader reader, Object model) throws Exception {
        try {
            return IOUtils.toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public void toggleResourceFilterState(RepoPath repoPath, boolean filtered) {
    }

    @Override
    public void assertLayoutConfigurationsBeforeSave(CentralConfigDescriptor newDescriptor) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        // Even that we are in the CoreAddonsImpl, it is not for sure that that we are on OSS, it might be that we got
        // the the CoreAddonsImpl because there is not yet license activated, so we got this bean from the
        // AddonManager#addonByType due to the missing license
        if (addonsManager.getArtifactoryRunningMode().isOss()) {
            List<RepoLayout> repoLayouts = newDescriptor.getRepoLayouts();
            if ((repoLayouts == null) || repoLayouts.isEmpty()) {
                throw new ConfigurationException("Could not find any repository layouts.");
            }

            if (repoLayouts.size() != 13) {
                throw new ConfigurationException(
                        "There should be 13 default repository layouts, but found " + repoLayouts.size());
            }

            assertLayoutsExistsAndEqual(repoLayouts, RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.IVY_DEFAULT,
                    RepoLayoutUtils.GRADLE_DEFAULT, RepoLayoutUtils.MAVEN_1_DEFAULT);
        }
    }

    @Override
    public boolean canCrossLayouts(RepoLayout source, RepoLayout target) {
        return false;
    }

    @Override
    public void performCrossLayoutMoveOrCopy(MoveMultiStatusHolder status, MoverConfig moverConfig,
            LocalRepo sourceRepo, LocalRepo targetLocalRepo, VfsItem sourceItem) {
        throw new UnsupportedOperationException(
                "Cross layout move or copy operations require the Repository Layouts addon.");
    }

    @Override
    public String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path) {
        return translateArtifactPath(sourceRepoLayout, targetRepoLayout, path, null);
    }

    @Override
    public String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path,
            @Nullable BasicStatusHolder multiStatusHolder) {
        return path;
    }

    private void assertLayoutsExistsAndEqual(List<RepoLayout> repoLayouts, RepoLayout... expectedLayouts) {
        for (RepoLayout expectedLayout : expectedLayouts) {
            assertLayoutExistsAndEqual(repoLayouts, expectedLayout);
        }
    }

    private void assertLayoutExistsAndEqual(List<RepoLayout> repoLayouts, RepoLayout expectedLayout) {

        if (!repoLayouts.contains(expectedLayout)) {
            throw new ConfigurationException("Could not find the default repository layout: " +
                    expectedLayout.getName());
        }

        RepoLayout existingLayoutConfig = repoLayouts.get(repoLayouts.indexOf(expectedLayout));
        if (!expectedLayout.equals(existingLayoutConfig)) {
            throw new ConfigurationException("The configured repository layout '" + expectedLayout.getName() +
                    "' is different from the default configuration.");
        }
    }

    @Override
    public BasicStatusHolder performRemoteReplication(RemoteReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    @Override
    public BasicStatusHolder performLocalReplication(LocalReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    @Override
    public void scheduleImmediateLocalReplicationTask(LocalReplicationDescriptor replicationDescriptor,
            BasicStatusHolder statusHolder) {
        statusHolder.error("Error: the replication addon is required for this operation.", HttpStatus.SC_BAD_REQUEST,
                log);
    }

    @Override
    public void scheduleImmediateRemoteReplicationTask(RemoteReplicationDescriptor replicationDescriptor,
            BasicStatusHolder statusHolder) {
        statusHolder.error("Error: the replication addon is required for this operation.", HttpStatus.SC_BAD_REQUEST,
                log);
    }

    @Override
    public ReplicationStatus getReplicationStatus(RepoPath repoPath) {
        return null;
    }

    @Override
    public void offerLocalReplicationDeploymentEvent(RepoPath repoPath, boolean isFile) {
    }

    @Override
    public void offerLocalReplicationMkDirEvent(RepoPath repoPath, boolean isFile) {
    }

    @Override
    public void offerLocalReplicationDeleteEvent(RepoPath repoPath, boolean isFile) {
    }

    @Override
    public void offerLocalReplicationPropertiesChangeEvent(RepoPath repoPath, boolean isFile) {
    }

    @Override
    public void validateTargetIsDifferentInstance(ReplicationBaseDescriptor descriptor,
            RealRepoDescriptor repoDescriptor) throws IOException {
    }

    @Override
    public void validateTargetLicense(ReplicationBaseDescriptor descriptor, RealRepoDescriptor repoDescriptor,
            int numOfReplicationConfigured) {
    }

    @Override
    public BasicStatusHolder filterIfMultiPushIsNotAllowed(List<LocalReplicationDescriptor> pushReplications) {
        BasicStatusHolder multiStatusHolder = new BasicStatusHolder();
        multiStatusHolder.error("Error: an HA license and the replication addon are required for this operation.",
                HttpStatus.SC_BAD_REQUEST, log);
        return multiStatusHolder;
    }

    @Override
    public void cleanupLocalReplicationProperties(LocalReplicationDescriptor replication) {

    }

    @Override
    public void handlePropagatedRemoteReplicationEvents(String originNode, ReplicationEventQueueWorkItem events) {

    }

    private BasicStatusHolder getReplicationRequiredStatusHolder() {
        BasicStatusHolder multiStatusHolder = new BasicStatusHolder();
        multiStatusHolder.error("Error: the replication addon is required for this operation.",
                HttpStatus.SC_BAD_REQUEST, log);
        return multiStatusHolder;
    }

    @Override
    public void requestRpmMetadataCalculation(LocalRepoDescriptor repo, String passphrase, boolean async,
            RepoPath... repoPaths) {

    }

    @Override
    public ArtifactRpmMetadata getRpmMetadata(FileInfo fileInfo) {
        return null;
    }

    @Override
    public void calculateVirtualYumMetadataAsync(RepoPath requestedPath, @Nullable RequestContext requestContext) {

    }

    @Override
    public void calculateVirtualYumMetadata(RepoPath requestedPath, @Nullable RequestContext requestContext) {

    }

    @Override
    public void invokeVirtualCalculationIfNeeded(RepoPath yumRepoRootPath) {

    }

    @Override
    public void recalculateAll(LocalRepoDescriptor localRepoDescriptor, String password, boolean delayed) {

    }

    @Override
    public void calculateMetadata(Set<DebianCalculationEvent> calculationRequests, boolean delayed) {

    }

    @Override
    public boolean hasPrivateKey() {
        return false;
    }

    @Override
    public boolean hasPublicKey() {
        return false;
    }

    @Override
    public void extractNuPkgInfo(FileInfo fileInfo, MutableStatusHolder statusHolder, boolean addToCache) {
    }

    @Override
    public void extractNuPkgInfoSynchronously(FileInfo file, MutableStatusHolder statusHolder) {
    }

    @Override
    public void addNuPkgToRepoCache(RepoPath repoPath, Properties properties) {
    }

    @Override
    public void addNuPkgToRepoCacheAsync(RepoPath repoPath, Properties properties) {

    }

    @Override
    public void removeNuPkgFromRepoCache(String repoKey, String packageId, String packageVersion) {
    }

    @Override
    public void internalAddNuPkgToRepoCache(RepoPath repoPath, Properties properties) {
    }

    @Override
    public void internalRemoveNuPkgFromRepoCache(String repoKey, String packageId, String packageVersion) {
    }

    @Nonnull
    @Override
    public RemoteRepo createRemoteRepo(InternalRepositoryService repoService, RemoteRepoDescriptor repoDescriptor,
            boolean offlineMode, RemoteRepo oldRemoteRepo) {
        return new HttpRepo((HttpRepoDescriptor) repoDescriptor, repoService, offlineMode, oldRemoteRepo);
    }

    @Override
    public void deployArchiveBundle(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException {
        response.sendError(HttpStatus.SC_BAD_REQUEST, "This REST API is available only in Artifactory Pro.", log);
    }

    @Override
    public InternalRequestContext getDynamicVersionContext(Repo repo, InternalRequestContext originalRequestContext,
            boolean isRemote) {
        return originalRequestContext;
    }

    @Override
    public boolean isCrowdAuthenticationSupported(Class<?> authentication) {
        return false;
    }

    @Override
    public Authentication authenticateCrowd(Authentication authentication) {
        throw new UnsupportedOperationException("This feature requires the Crowd SSO addon.");
    }

    @Override
    public boolean findUser(String userName) {
        return false;
    }

    @Override
    public void addPod(FileInfo info) {

    }

    @Override
    public void addPodAfterCommit(FileInfo info) {

    }

    @Override
    public void cachePodProperties(FileInfo info) {

    }

    @Override
    public void removePod(FileInfo info, Properties properties) {

    }

    @Override
    public void reindexAsync(String repoKey) {

    }

    @Override
    @Nonnull
    public PuppetInfo getPuppetInfo(RepoPath repoPath) {
        return null;
    }

    @Override
    public boolean isPuppetFile(RepoPath repoPath) {
        return false;
    }

    @Override
    public NpmMetadataInfo getNpmMetaDataInfo(RepoPath repoPath) {
        return null;
    }

    @Override
    public void addPuppetPackage(FileInfo fileInfo) {

    }

    @Override
    public void removePuppetPackage(FileInfo fileInfo) {

    }

    @Override
    public void reindexPuppetRepo(String repoKey) {

    }

    @Override
    public void afterRepoInit(String repoKey) {
    }

    @Override
    public ArtifactGemsInfo getGemsInfo(String repoKey, String path) {
        return null;
    }

    @Override
    public void requestAsyncReindexNuPkgs(String repoKey) {
    }

    @Override
    public boolean isHaEnabled() {
        return false;
    }

    @Override
    public boolean isPrimary() {
        return true;
    }

    @Override
    public boolean isHaConfigured() {
        try {
            return ArtifactoryHome.get().isHaConfigured();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void notifyAsync(HaMessageWorkItem workItem) {

    }

    @Override
    public void notify(HaMessageTopic haMessageTopic, HaMessage haMessage) {
    }

    @Override
    public String getHostId() {
        return HttpUtils.getHostId();
    }

    @Override
    public void updateArtifactoryServerRole() {
    }

    @Override
    public void propagateLicenseChanges() {

    }

    @Override
    public ConflictGuard getConflictGuard(String key) {
        return getConflictGuardProvider().getConflictGuard(key);
    }

    @Override
    public void propagateReplicationListener(ReplicationOwnerModel replicationChannelModel) {

    }

    @Override
    public void propagateRemoveReplicationListener(ReplicationOwnerModel replicationChannelModel) {

    }

    @Override
    public void propagateReplicationEvents(String target, ReplicationEventQueueWorkItem queue) {

    }

    @Override
    public boolean deleteArtifactoryServer(String serverId) {
        return false;
    }

    @Override
    public boolean artifactoryServerHasHeartbeat(ArtifactoryServer artifactoryServer) {
        return false;
    }

    @Override
    public void propagateTaskToPrimary(Task Task) {
    }

    @Override
    public void initConfigBroadcast(ArtifactoryApplicationContext context) {

    }

    @Override
    public FsItemsVault getFsFileItemVault() {
        return getConflictGuardProvider().getFileLockingMap();
    }

    @Override
    public FsItemsVault getFsFolderItemVault() {
        return getConflictGuardProvider().getFolderLockingMap();
    }

    @Override
    public boolean isHaAuthentication() {
        return false;
    }

    @Override
    public void propagateConfigReload() {
    }

    @Override
    public void handleIncomingPropagationEvent(String topic, HaMessage haMessage) {
        throw new RuntimeException("Propagation is only supported on HA. Ignoring incoming propagation event");
    }

    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public <T> ConflictsGuard<T> getConflictsGuard(String mapName) {
        return getConflictGuardProvider().getConflictsGuard(mapName);
    }

    @Override
    public String getCurrentMemberServerId() {
        return null;
    }

    @Override
    public void propagateDebianUpdateCache(RepoPath path) {
    }

    @Override
    public void propagateOpkgReindexAll(ArtifactoryServer server, String repoKey, boolean async, boolean writeProps) {

    }

    /**
     * Tell other node to activate a license with excluding a specific license
     *
     * @param server      The server to communicate with
     * @param skipLicense The licenses to skip while trying to activate a license
     */
    @Override
    public void propagateActivateLicense(ArtifactoryServer server, Set<String> skipLicense) {
    }

    @Override
    public void propagatePluginReload() {

    }

    @Override
    public List<String> propagateSupportBundleListRequest() {
        return Collections.emptyList();
    }

    @Override
    public boolean propagateDeleteSupportBundle(String bundleName) {
        return false;
    }

    @Override
    public InputStream propagateSupportBundleDownloadRequest(String bundleName, String nodeId) {
        return null;
    }

    @Override
    public UIDeployPropagationResult propagateUiUploadRequest(String nodeId, String payload) {
        return null;
    }

    @Override
    public void propagateArtifactoryEncryptionKeyChanged() {

    }

    @Override
    public <T> List<T> propagateTrafficCollector(long startLong, long endLong, List<String> ipsToFilter,
            List<ArtifactoryServer> servers, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> propagateTasksList(List<ArtifactoryServer> servers, Class<T> clazz) {
        return null;
    }

    @Override
    public void forceOptimizationOnce() {
    }

    @Override
    public <K, V> Map<K, V> getSessionMap(String mapName) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<ArtifactoryServer> getAllArtifactoryServers() {
        return new ArrayList<>();
    }

    @Override
    public void addNpmPackage(FileInfo info) {

    }

    @Override
    public void handleAddAfterCommit(FileInfo info) {

    }

    @Override
    public void removeNpmPackage(FileInfo info) {

    }

    @Override
    public void reindex(LocalRepoDescriptor descriptor, boolean async) {

    }

    @Override
    public PypiPkgMetadata getPypiMetadata(RepoPath packagePath) {
        return null;
    }

    @Override
    public boolean isPypiFile(RepoPath repoPath) {
        return false;
    }

    @Override
    public void addBowerPackage(FileInfo info, String version) {

    }

    @Override
    public void handleAddAfterCommit(FileInfo info, String version) {

    }

    @Override
    public void removeBowerPackage(FileInfo info) {

    }

    @Override
    public boolean isBowerFile(String filePath) {
        return false;
    }

    @Override
    public void addHelmPackage(VfsItem fsItem) {

    }

    @Override
    public void removeHelmPackage(VfsItem fsItem) {

    }

    @Override
    public HelmMetadataInfo getMetadataToUiModel(RepoPath repoPath) {
        return null;
    }

    @Override
    public void requestHelmMetadataCalculation(RepoPath path, boolean async) {

    }

    @Override
    public RepoPath requestVirtualHelmCustomMetadataCalculation(RepoPath path, String requestUrl) {
        return path;
    }

    @Override
    public void requestReindexRepo(String repoKey, boolean async) {

    }

    @Override
    public void invokeVirtualMetadataEviction(RealRepoDescriptor descriptor) {
    }

    @Override
    public void requestAsyncReindexBowerPackages(String repoKey) {

    }

    @Override
    public BowerMetadataInfo getBowerMetadata(RepoPath repoPath) {
        return null;
    }

    @Override
    public void pushTagToBintray(String repoKey, BintrayDockerPushRequest request, @Nullable String distRepoKey) {

    }

    @Override
    public DockerV2InfoModel getDockerV2Model(RepoPath manifestPath) throws IOException {
        return null;
    }

    @Override
    public String fetchDockerAuthToken(DockerTokenCacheKey tokenCacheKey) {
        return null;
    }

    @Override
    public Map<RepoPath, WatchersInfo> getAllWatchers(RepoPath repoPath) {
        return null;
    }

    @Override
    public void removeWatcher(RepoPath repoPath, String watchUser) {

    }

    @Override
    public void addWatcher(RepoPath repoPath, String watcherUsername) {

    }

    @Override
    public boolean isUserWatchingRepo(RepoPath repoPath, String userName) {
        return false;
    }

    @Override
    public Pair<RepoPath, WatchersInfo> getNearestWatchDefinition(RepoPath repoPath, String userName) {
        return null;
    }

    @Override
    public WatchersInfo getWatchers(RepoPath repoPath) {
        return null;
    }

    @Override
    public Set<ArtifactoryBuildArtifact> getBuildArtifactsFileInfos(Build build) {
        return null;
    }

    @Override
    public Map<Dependency, FileInfo> getBuildDependenciesFileInfos(Build build) {
        return null;
    }

    @Override
    public Set<BuildId> getLatestBuildIDsPaging(String offset, String orderBy, String direction, String limit) {
        return null;
    }

    @Override
    public List<GeneralBuild> getBuildForNamePaging(String buildName, String orderBy, String direction, String offset,
            String limit) throws SQLException {
        return null;
    }

    @Override
    public int getBuildForNameTotalCount(String buildName) throws SQLException {
        return 0;
    }

    @Override
    public Build getBuild(BuildRun buildRun) {
        return null;
    }

    @Override
    public BuildRun getBuildRun(String buildName, String buildNumber, String buildStarted) {
        return null;
    }

    @Override
    public Build getLatestBuildByNameAndNumber(String buildName, String BuildNumber) {
        return null;
    }

    @Override
    public List<PublishedModule> getPublishedModules(String buildName, String date, String orderBy, String direction,
            String offset, String limit) {
        return null;
    }

    @Override
    public int getPublishedModulesCounts(String buildName, String date) {
        return 0;
    }

    @Override
    public List<ModuleArtifact> getModuleArtifact(String buildName, String buildNumber, String moduleId, String date,
            String orderBy, String direction, String offset, String limit) {
        return new ArrayList<>();
    }

    @Override
    public int getModuleArtifactCount(String buildNumber, String moduleId, String date) {
        return 0;
    }

    @Override
    public List<ModuleDependency> getModuleDependency(String buildNumber, String moduleId, String date, String orderBy,
            String direction, String offset, String limit) {
        return null;
    }

    @Override
    public int getModuleDependencyCount(String buildNumber, String moduleId, String date) {
        return 0;
    }

    @Override
    public void deleteAllBuilds(String name) {

    }

    @Override
    public BuildsDiff getBuildsDiff(Build firstBuild, Build secondBuild, String baseStorageInfoUri) {
        return null;
    }

    @Override
    public List<BuildsDiffBaseFileModel> compareArtifacts(Build build, Build secondBuild) {
        return null;
    }

    @Override
    public List<BuildsDiffBaseFileModel> compareDependencies(Build build, Build secondBuild) {
        return null;
    }

    @Override
    public List<BuildsDiffPropertyModel> compareProperties(Build build, Build secondBuild) {
        return null;
    }

    @Override
    public NuMetaData getNutSpecMetaData(RepoPath nuGetRepoPath) {
        return null;
    }

    @Override
    public Set<LdapUserGroup> refreshLdapGroups(String userName, LdapGroupSetting ldapGroupSetting,
            BasicStatusHolder statusHolder) {
        return null;
    }

    @Override
    public int importLdapGroupsToArtifactory(List ldapGroups, LdapGroupPopulatorStrategies strategy) {
        return 0;
    }

    @Override
    public String[] retrieveUserLdapGroups(String userName, LdapGroupSetting ldapGroupSetting) {
        return null;
    }

    @Override
    public KeyStore loadKeyStore(File keyStoreFile, String password) {
        return null;
    }

    @Override
    public String getKeystorePassword() {
        return null;
    }

    @Override
    public KeyStore getExistingKeyStore() {
        return null;
    }

    @Override
    public Key getAliasKey(KeyStore keyStore, String alias, String password) {
        return null;
    }

    @Override
    public void addKeyPair(File file, String pairName, String keyStorePassword, String alias, String privateKeyPassword)
            throws IOException {

    }

    @Override
    public boolean keyStoreExist() {
        return false;
    }

    @Override
    public List<String> getKeyPairNames() {
        return null;
    }

    @Override
    public List<String> getSslCertNames(){
        return null;
    }

    @Override
    public boolean hasKeyPair(String keyPairName) {
        return false;
    }

    @Override
    public boolean removeKeyPair(String keyPairName) {
        return false;
    }

    @Override
    public void setKeyStorePassword(String password) {

    }

    @Override
    public void removeKeyStorePassword() {
    }

    @Override
    public void addPemCertificateToKeystore(String pemContent, String alias) {
        throw new UnsupportedOperationException(
                "Client certificate is only available on licensed Artifactory versions.");
    }

    @Override
    public String getSamlLoginIdentityProviderUrl(HttpServletRequest request) {
        return null;
    }

    @Override
    public void createCertificate(String certificate) throws Exception {

    }

    @Override
    public Boolean isSamlAuthentication(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return false;
    }

    @Override
    public boolean isSamlAuthentication() {
        return false;
    }

    @Override
    public boolean supportRemoteStats() {
        return false;
    }

    @Override
    public void fileDownloadedRemotely(StatsInfo statsInfo, String origin, RepoPath repoPath) {

    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void addPropertySha256RecursivelyMultiple(RepoPath repoPath) {
    }

    @Override
    public boolean isSupportAddonEnabled() {
        return false;
    }

    @Override
    public List<String> generate(Object bundleConfiguration) {
        return Lists.newArrayList();
    }

    @Override
    public List<String> list() {
        return Lists.newArrayList();
    }

    @Override
    public List<String> listFromThisServer() {
        return Lists.newLinkedList();
    }

    @Override
    public InputStream download(String bundleName, String handlingNode) throws FileNotFoundException {
        return null;
    }

    @Override
    public boolean delete(String bundleName, boolean shouldPropagate, boolean async) {
        return false;
    }

    @Override
    public boolean isGitLfsCommand(String command) {
        return false;
    }

    @Override
    public Command createGitLfsCommand(String command, SshTokenManager sshTokenManager) {
        return null;
    }

    @Override
    public void recalculateEntireRepo(String repoKey, String password, boolean delayed, boolean writeProps) {

    }

    @Override
    public void calculateOpkgMetadata(Set<OpkgCalculationEvent> calculationRequests, boolean delayed) {

    }

    @Override
    public boolean isHttpSsoAuthentication() {
        return false;
    }

    @Override
    public boolean foundExpiredAndRemoteIsNewer(RepoResource remoteResource, RepoResource cachedResource) {
        return new CacheExpiryStrategyImpl().foundExpiredAndRemoteIsNewer(remoteResource, cachedResource);
    }

    @Override
    public DebianMetadataInfo getDebianMetadataInfo(RepoPath repoPath) {
        return null;
    }

    @Override
    public boolean isXrayConfigExist() {
        return false;
    }

    @Override
    public boolean isXrayEnabled() {
        return false;
    }

    @Override
    public InputStream scanBuild(XrayScanBuild xrayScanBuild) throws IOException {
        return null;
    }

    @Override
    public String createXrayUser() {
        return null;
    }

    @Override
    public void removeXrayConfig() {

    }

    @Override
    public void deleteXrayUser(String xrayUser) {
    }

    @Override
    public void setXrayEnabledOnRepos(List<XrayRepo> repos, boolean add) {
    }

    @Override
    public void updateXraySelectedIndexedRepos(List<XrayRepo> repos) {
    }

    @Override
    public int getXrayIndexedCountForRepo(String repoKey) {
        return 0;
    }

    @Override
    public int getXrayIndexingCountForRepo(String repoKey) {
        return 0;
    }

    @Override
    public int getXrayPotentialCountForRepo(String repoKey) {
        return 0;
    }

    @Override
    public List<XrayRepo> getXrayIndexedAndNonIndexed(boolean indexed) {
        return Lists.newArrayList();
    }

    @Override
    public void callAddBuildInterceptor(Build build) {
    }

    @Override
    public void callDeleteBuildInterceptor(String buildName, String buildNumber) {
    }

    @Override
    public void indexRepos(List<String> repos) {
    }

    @Override
    public void clearAllIndexTasks() {
    }

    @Override
    public void blockXrayGlobally() {
        throw new UnsupportedOperationException(
                "Block Xray globally requires the Xray resources add-on.");
    }

    @Override
    public void unblockXrayGlobally() throws OperationNotSupportedException {
        throw new UnsupportedOperationException(
                "Unblock Xray globally requires the Xray resources add-on.");
    }

    @Nonnull
    @Override
    public ArtifactXrayInfo getArtifactXrayInfo(@Nonnull RepoPath path) {
        return ArtifactXrayInfo.EMPTY;
    }

    @Override
    public void handlePackageDeployment(RepoPath repoPath) {

    }

    @Override
    public void handlePackageDeletion(RepoPath repoPath) {

    }

    @Override
    public void recalculateAll(String repoKey, boolean async) {

    }

    @Override
    public void calculateVirtualRepoMetadata(String repoKey, String baseUrl) {

    }

    @Override
    public boolean isComposerSupportedExtension(String fileName) {
        return false;
    }

    @Override
    public ComposerMetadataInfo getComposerMetadataInfo(RepoPath repoPath) {
        return null;
    }

    @Override
    public boolean isConanReferenceFolder(@Nonnull RepoPath repoPath) {
        return false;
    }

    @Override
    public boolean isConanPackageFolder(@Nonnull RepoPath repoPath) {
        return false;
    }

    @Nonnull
    @Override
    public ConanRecipeInfo getRecipeInfo(@Nonnull RepoPath repoPath) {
        throw new IllegalArgumentException("Not supported");
    }

    @Nonnull
    @Override
    public ConanPackageInfo getPackageInfo(@Nonnull RepoPath repoPath) {
        throw new IllegalArgumentException("Not supported");
    }

    @Override
    public int countPackages(@Nonnull RepoPath repoPath) {
        return 0;
    }

    @Override
    public boolean isDownloadBlocked(RepoPath path) {
        return false;
    }

    @Override
    public boolean isHandledByXray(RepoPath path) {
        return false;
    }

    @Override
    public void setAlertIgnored(boolean ignored, RepoPath path) {

    }

    @Override
    public List<RepoPath> getBlockedPathsUnder(RepoPath folder) {
        return null;
    }

    @Override
    public void addCookbook(FileInfo info) {

    }

    @Override
    public ChefCookbookInfo getChefCookbookInfo(RepoPath repoPath) {
        return null;
    }

    @Override
    public boolean isChefCookbookFile(String fileName) {
        return false;
    }

    @Override
    public ReleaseBundleResult handleRequest(ReleaseBundleRequest bundleRequest) {
        throw new UnsupportedOperationException(
                "Release bundle is only available on Enterprise licensed Artifactory instances.");
    }

    @Override
    public ReplicationChannel establishReplicationChannel(ReplicationOwnerModel replicationOwnerModel,
            String targetNode) {
        return null;
    }

    @Override
    public void removeReplicationChannel(ReplicationOwnerModel replicationOwnerModel) {

    }

    @Override
    public BasicStatusHolder distributeArtifact(FileSpec fileSpec, String AuthToken) {
        return null;
    }
}

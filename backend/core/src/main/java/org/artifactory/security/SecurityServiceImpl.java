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

package org.artifactory.security;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.ha.message.HaBaseMessage;
import org.artifactory.addon.ha.workitem.HaAclMessageWorkItem;
import org.artifactory.addon.sso.HttpSsoAddon;
import org.artifactory.addon.sso.saml.SamlSsoAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.*;
import org.artifactory.api.security.ldap.LdapService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.config.ConfigurationException;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.security.*;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.descriptor.security.sso.SamlSettings;
import org.artifactory.exception.InvalidNameException;
import org.artifactory.exception.ValidationException;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.io.checksum.ChecksumUtils;
import org.artifactory.model.xstream.security.ImmutableAclInfo;
import org.artifactory.repo.*;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.security.SecurityConstants;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.security.access.AccessService;
import org.artifactory.security.access.AccessTokenAuthentication;
import org.artifactory.security.access.ArtifactoryRepoPathScopeToken;
import org.artifactory.security.access.emigrate.AccessSecurityEmigrator;
import org.artifactory.security.exceptions.LoginDisabledException;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.security.exceptions.PasswordExpireException;
import org.artifactory.security.exceptions.UserLockedException;
import org.artifactory.security.interceptor.ApiKeysEncryptor;
import org.artifactory.security.interceptor.BintrayAuthEncryptor;
import org.artifactory.security.interceptor.SecurityConfigurationChangesInterceptors;
import org.artifactory.security.interceptor.UserPasswordEncryptor;
import org.artifactory.security.jobs.CredentialsWatchJob;
import org.artifactory.security.jobs.PasswordExpireNotificationJob;
import org.artifactory.security.log.AuditLogger;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.DockerTokenManager;
import org.artifactory.security.props.auth.PropsAuthenticationToken;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.artifactory.security.providermgr.ArtifactoryTokenProvider;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.security.service.AclCache;
import org.artifactory.storage.security.service.AclStoreService;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.storage.security.service.UserLockInMemoryService;
import org.artifactory.update.security.SecurityInfoReader;
import org.artifactory.update.security.SecurityVersion;
import org.artifactory.util.*;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.security.crypto.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.artifactory.security.ArtifactoryPermission.DEPLOY;
import static org.artifactory.security.ArtifactoryPermission.READ;
import static org.artifactory.security.PermissionTargetInfo.*;

@Service
@Reloadable(beanClass = InternalSecurityService.class,
        initAfter = {InternalCentralConfigService.class, DbService.class, AccessService.class},
        listenOn = CentralConfigKey.security)
public class SecurityServiceImpl implements InternalSecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private static final String DELETE_FOR_SECURITY_MARKER_FILENAME = ".deleteForSecurityMarker";
    private static final int MAX_SOURCES_TO_TRACK = 10000;
    private static final int MIN_DELAY_BETWEEN_FORGOT_PASSWORD_ATTEMPTS_PER_SOURCE = 500; // ms
    public static final int MAX_USERS_TO_TRACK = 10000; // max locked users to keep in cache

    // cache meaning  <userName, incorrect-login-timestampts>
    private final Cache<String, List<Long>> unknownUsersCache = CacheBuilder.newBuilder()
            .maximumSize(MAX_USERS_TO_TRACK).
                    expireAfterWrite(1, TimeUnit.HOURS).build();

    private Cache<String, List<Long>> resetPasswordAttemptsBySourceCache;

    @Autowired
    private DockerTokenManager dockerTokenManager;
    @Autowired
    private AccessService accessService;
    @Autowired
    private AclStoreService aclStoreService;
    @Autowired
    private UserGroupStoreService userGroupStoreService;
    @Autowired
    private CentralConfigService centralConfig;
    @Autowired
    private InternalRepositoryService repositoryService;
    @Autowired
    private MailService mailService;
    @Autowired
    private AddonsManager addons;
    @Autowired
    private LdapService ldapService;
    @Autowired
    private SecurityConfigurationChangesInterceptors interceptors;
    @Autowired
    private CachedThreadPoolTaskExecutor executor;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ApiKeysEncryptor apiKeysEncryptor;
    @Autowired
    private UserPasswordEncryptor userPasswordEncryptor;
    @Autowired
    private BintrayAuthEncryptor bintrayAuthEncryptor;
    @Autowired
    private AuditLogger auditLog;
    @Autowired
    private UserPassAuthenticationProvider userPassAuthenticationProvider;
    @Autowired
    private AccessSecurityEmigrator securityEmigrator;
    @Autowired
    private UserLockInMemoryService userLockInMemoryService;
    @Autowired
    private ApiKeyManager apiKeyManager;

    private InternalArtifactoryContext context;

    private TreeSet<SecurityListener> securityListeners = new TreeSet<>();

    /**
     * @param user The authentication token.
     * @return An array of sids of the current user and all it's groups.
     */
    private static Set<ArtifactorySid> getUserEffectiveSids(SimpleUser user) {
        Set<ArtifactorySid> sids = new HashSet<>(2);
        Set<UserGroupInfo> groups = user.getDescriptor().getGroups();
        // add the current user
        sids.add(new ArtifactorySid(user.getUsername(), false));
        // add all the groups the user is a member of
        for (UserGroupInfo group : groups) {
            sids.add(new ArtifactorySid(group.getGroupName(), true));
        }
        return sids;
    }

    private static boolean isAdmin(Authentication authentication) {
        return isAuthenticated(authentication) && getSimpleUser(authentication).isEffectiveAdmin();
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private static SimpleUser getSimpleUser(Authentication authentication) {
        return (SimpleUser) authentication.getPrincipal();
    }

    private static boolean matches(PermissionTargetInfo aclPermissionTarget, String path, boolean folder) {
        return PathMatcher.matches(path, aclPermissionTarget.getIncludes(), aclPermissionTarget.getExcludes(), folder);
    }

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    @Override
    public void init() {
        // if we need to dump the current security config (.deleteForSecurityMarker doesn't exist)
        // and unlock all admin users

        if (shouldRestoreLoginCapabilities()) {
            dumpCurrentSecurityConfig();
            unlockAdminUsers();
        }

        //Locate and import external configuration file
        checkForExternalConfiguration();
        CoreAddons coreAddon = addons.addonByType(CoreAddons.class);
        if (coreAddon.isCreateDefaultAdminAccountAllowed() && !userGroupStoreService.adminUserExists()) {
            createDefaultAdminUser();
        }
        createDefaultAnonymousUser();

        // start CredentialsWatchJob
        TaskBase credentialsWatchJob = TaskUtils.createRepeatingTask(CredentialsWatchJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.passwordExpireJobIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(30L));
        taskService.startTask(credentialsWatchJob, false);

        // start PasswordExpireNotificationJob
        TaskBase passwordExpireNotificationJob = TaskUtils.createRepeatingTask(PasswordExpireNotificationJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.passwordExpireNotificationJobIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(60L));
        taskService.startTask(passwordExpireNotificationJob, false);

        initResetPasswordCache(getPasswordResetPolicy(centralConfig.getDescriptor()));
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Need to check if security conf changed then clear security caches
        if (!centralConfig.getDescriptor().getSecurity().equals(oldDescriptor.getSecurity())) {
            clearSecurityListeners();
            // Need to check if password reset policy changed
            PasswordResetPolicy passwordResetPolicy = getPasswordResetPolicy(centralConfig.getDescriptor());
            if (!passwordResetPolicy.equals(getPasswordResetPolicy(oldDescriptor))) {
                initResetPasswordCache(passwordResetPolicy);
            }
        }
    }

    private void initResetPasswordCache(PasswordResetPolicy passwordResetPolicy) {
        resetPasswordAttemptsBySourceCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SOURCES_TO_TRACK)
                .expireAfterWrite(passwordResetPolicy.getTimeToBlockInMinutes(), TimeUnit.MINUTES).build();
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        return getAccessEmigrateMarkerFile().exists() ||
                InternalSecurityService.super.isInterested(source, target);
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        if (source.getVersion().beforeOrEqual(ArtifactoryVersion.v480) &&
                target.getVersion().after(ArtifactoryVersion.v480)) {
            if (CryptoHelper.hasArtifactoryKey(ArtifactoryHome.get())) {
                apiKeysEncryptor.encryptOrDecryptAsynchronously(true);
            }
        }
        if (source.getVersion().beforeOrEqual(ArtifactoryVersion.v4112) &&
                target.getVersion().after(ArtifactoryVersion.v4112)) {
            if (CryptoHelper.hasArtifactoryKey(ArtifactoryHome.get())) {
                userPasswordEncryptor.encryptOrDecryptAsynchronously(true);
            }
        }
        if (source.getVersion().afterOrEqual(ArtifactoryVersion.v300) &&
                source.getVersion().before(ArtifactoryVersion.v521m006) &&
                target.getVersion().afterOrEqual(ArtifactoryVersion.v521m006)) {
            if (CryptoHelper.hasArtifactoryKey(ArtifactoryHome.get())) {
                bintrayAuthEncryptor.encryptOrDecryptAsynchronously(true);
            }
        }

        File accessEmigrateMarkerFile = getAccessEmigrateMarkerFile();
        if (accessEmigrateMarkerFile.exists()) {
            securityEmigrator.emigrate();
            if (!accessEmigrateMarkerFile.delete()) {
                throw new IllegalStateException("Could not delete marker file! " +
                        "File must be deleted for startup to complete. [" +
                        accessEmigrateMarkerFile.getAbsolutePath() + "]");
            }
        }
    }

    private File getAccessEmigrateMarkerFile() {
        return ArtifactoryHome.get().getAccessEmigrateMarkerFile();
    }

    private void dumpCurrentSecurityConfig() {
        try {
            if (accessService.getAccessClient() == null) {
                throw new IllegalStateException("Access client isn't initialized yet");
            }
            accessService.getAccessClient().system().exportAccessServer();
            log.debug("Successfully dumped access security file");

            createSecurityDumpMarkerFile();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't export access server", e);
        }
    }

    /**
     * @return true if SecurityDumpMarkerFile is unavailable
     * otherwise false
     */
    private boolean shouldRestoreLoginCapabilities() {
        File deleteForConsistencyFix = getSecurityDumpMarkerFile();
        return !deleteForConsistencyFix.exists();
    }

    /**
     * Creates/recreates the file that enabled security descriptor dump.
     * Also checks we have proper write access to the data folder.
     */
    private void createSecurityDumpMarkerFile() {
        File securityDumpMarkerFile = getSecurityDumpMarkerFile();
        try {
            securityDumpMarkerFile.createNewFile();
        } catch (IOException e) {
            log.debug("Could not create file: '" + securityDumpMarkerFile.getAbsolutePath() + "'.", e);
        }
    }

    private File getSecurityDumpMarkerFile() {
        return new File(ArtifactoryHome.get().getDataDir(), DELETE_FOR_SECURITY_MARKER_FILENAME);
    }

    /**
     * Checks for an externally supplied configuration file ($ARTIFACTORY_HOME/etc/security.xml). If such a file is
     * found, it will be deserialized to a security info (descriptor) object and imported to the system. This option is
     * to be used in cases like when an administrator is locked out of the system, etc'.
     */
    private void checkForExternalConfiguration() {
        ArtifactoryContext ctx = ContextHelper.get();
        final File etcDir = ctx.getArtifactoryHome().getEtcDir();
        final File configurationFile = new File(etcDir, "security.import.xml");
        //Work around Jackrabbit state visibility issues within the same tx by forking a separate tx (RTFACT-4526)
        Callable callable = () -> {
            String configAbsolutePath = configurationFile.getAbsolutePath();
            if (configurationFile.isFile()) {
                if (!configurationFile.canRead() || !configurationFile.canWrite()) {
                    throw new ConfigurationException(
                            "Insufficient permissions. Security configuration import requires " +
                                    "both read and write permissions for " + configAbsolutePath
                    );
                }
                try {
                    SecurityInfo descriptorToSave = new SecurityInfoReader().read(configurationFile);
                    //InternalSecurityService txMe = ctx.beanForType(InternalSecurityService.class);
                    getAdvisedMe().importSecurityData(descriptorToSave);
                    Files
                            .switchFiles(configurationFile, new File(etcDir, "security.bootstrap.xml"));
                    log.info("Security configuration imported successfully from " + configAbsolutePath + ".");
                } catch (Exception e) {
                    throw new IllegalArgumentException("An error has occurred while deserializing the file " +
                            configAbsolutePath +
                            ". Please assure it's validity or remove it from the 'etc' folder.", e);
                }
            }
            return null;
        };
        @SuppressWarnings("unchecked") Future<Set<String>> future = executor.submit(callable);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Could not import external security config.", e);
        }
    }

    @Override
    public boolean isAnonymous() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && isAnonymousUser(authentication.getName());
    }

    @Override
    public boolean requireProfileUnlock() {
        SecurityDescriptor security = ContextHelper.get().getCentralConfig().getDescriptor().getSecurity();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        boolean allowSsoAuth = isAllowSsoAuth(security, addonsManager);
        boolean allowSamlAuth = isAllowSamlAuth(security, addonsManager);
        boolean allowPropsAuth = isAllowPropsAuth(security);
        return !(allowPropsAuth || allowSamlAuth || allowSsoAuth);
    }

    private boolean isAllowPropsAuth(SecurityDescriptor security) {
        OAuthSettings oauthSettings = security.getOauthSettings();
        boolean allowUserToAccessProfileOauth = (oauthSettings != null) && oauthSettings.isAllowUserToAccessProfile();
        return AuthenticationHelper.getAuthentication() instanceof PropsAuthenticationToken && allowUserToAccessProfileOauth;
    }

    private boolean isAllowSamlAuth(SecurityDescriptor security, AddonsManager addonsManager) {
        SamlSettings samlSettings = security.getSamlSettings();
        boolean allowUserToAccessProfileSaml = (samlSettings != null) && samlSettings.isAllowUserToAccessProfile();
        return addonsManager.addonByType(SamlSsoAddon.class).isSamlAuthentication() && allowUserToAccessProfileSaml;
    }

    private boolean isAllowSsoAuth(SecurityDescriptor security, AddonsManager addonsManager) {
        HttpSsoSettings httpSsoSettings = security.getHttpSsoSettings();
        boolean allowUserToAccessProfileSso = (httpSsoSettings != null) && httpSsoSettings.isAllowUserToAccessProfile();
        return addonsManager.addonByType(HttpSsoAddon.class).isHttpSsoAuthentication() && allowUserToAccessProfileSso;
    }

    @Override
    public boolean requireProfilePassword() {
        UserInfo userInfo = findUser(currentUsername(), false);
        boolean userHasPassword = false;
        if (userInfo != null && userInfo.getPassword() != null) {
            userHasPassword = userInfo.getPassword().length() > 0;
        }
        SecurityDescriptor security = ContextHelper.get().getCentralConfig().getDescriptor().getSecurity();
        HttpSsoSettings httpSsoSettings = security.getHttpSsoSettings();
        boolean allowUserToAccessProfileSso =
                httpSsoSettings != null && httpSsoSettings.isAllowUserToAccessProfile();
        SamlSettings samlSettings = security.getSamlSettings();
        boolean allowUserToAccessProfileSaml = (samlSettings != null) && samlSettings.isAllowUserToAccessProfile();
        OAuthSettings oauthSettings = security.getOauthSettings();
        boolean allowUserToAccessProfileOauth =
                (oauthSettings != null) && oauthSettings.isAllowUserToAccessProfile();
        Authentication authentication = AuthenticationHelper.getAuthentication();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SamlSsoAddon samlSsoAddon = addonsManager.addonByType(SamlSsoAddon.class);
        HttpSsoAddon httpSsoAddon = addonsManager.addonByType(HttpSsoAddon.class);
        return !doNotRequirePasswordFromExtUser(userHasPassword, allowUserToAccessProfileSso,
                allowUserToAccessProfileSaml, allowUserToAccessProfileOauth,
                authentication, samlSsoAddon, httpSsoAddon);
    }

    private boolean doNotRequirePasswordFromExtUser(boolean userHasPassword, boolean allowUserToAccessProfileSso,
            boolean allowUserToAccessProfileSaml, boolean allowUserToAccessProfileOauth, Authentication authentication,
            SamlSsoAddon samlSsoAddon, HttpSsoAddon httpSsoAddon) {
        return ((authentication instanceof PropsAuthenticationToken && !allowUserToAccessProfileOauth) ||
                (samlSsoAddon.isSamlAuthentication() && !allowUserToAccessProfileSaml) ||
                (httpSsoAddon.isHttpSsoAuthentication() && !allowUserToAccessProfileSso)) && !userHasPassword;
    }


    @Override
    public boolean isAnonAccessEnabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security.isAnonAccessEnabled();
    }

    private boolean isAnonBuildInfoAccessDisabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security.isAnonAccessToBuildInfosDisabled();
    }

    @Override
    public boolean isAnonUserAndAnonBuildInfoAccessDisabled() {
        return isAnonymous() && isAnonBuildInfoAccessDisabled();
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAuthenticated(authentication);
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAdmin(authentication);
    }

    @Override
    public void createAcl(MutableAclInfo aclInfo) {
        assertAdmin();
        if (StringUtils.isEmpty(aclInfo.getPermissionTarget().getName())) {
            throw new IllegalArgumentException("ACL name cannot be null");
        }

        MutableAclInfo compatibleAcl = makeNewAclRemoteRepoKeysAclCompatible(aclInfo);
        cleanupAclInfo(compatibleAcl);
        aclStoreService.createAcl(compatibleAcl);
        interceptors.onPermissionsAdd(compatibleAcl);
        notifyClusterMembers();
    }

    @Override
    public void updateAcl(MutableAclInfo acl) {
        //If the editing user is not a sys-admin
        if (!isAdmin()) {
            //Assert that no unauthorized modifications were performed
            validateUnmodifiedPermissionTarget(acl.getPermissionTarget());
        }

        MutableAclInfo compatibleAcl = makeNewAclRemoteRepoKeysAclCompatible(acl);

        // Removing empty Ace
        cleanupAclInfo(compatibleAcl);
        aclStoreService.updateAcl(compatibleAcl);
        interceptors.onPermissionsUpdate(compatibleAcl);
        notifyClusterMembers();
    }

    @Override
    public void deleteAcl(PermissionTargetInfo target) {
        aclStoreService.deleteAcl(target.getName());
        interceptors.onPermissionsDelete(target.getName());
        notifyClusterMembers();
    }

    private void notifyClusterMembers() {
        HaAddon haAddon = addons.addonByType(HaAddon.class);
        HaBaseMessage haMessage = new HaBaseMessage(haAddon.getCurrentMemberServerId());
        HaAclMessageWorkItem workItem = new HaAclMessageWorkItem(haMessage);
        haAddon.notifyAsync(workItem);
    }

    @Override
    public List<PermissionTargetInfo> getPermissionTargets(ArtifactoryPermission permission) {
        return getPermissionTargetsByPermission(permission);
    }

    private List<PermissionTargetInfo> getPermissionTargetsByPermission(ArtifactoryPermission permission) {
        List<PermissionTargetInfo> result = new ArrayList<>();
        Collection<AclInfo> allAcls = aclStoreService.getAllAcls();
        for (AclInfo acl : allAcls) {
            if (hasPermissionOnAcl(acl, permission)) {
                result.add(acl.getPermissionTarget());
            }
        }
        return result;
    }

    @Override
    public boolean isUpdatableProfile() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isUpdatableProfile();
    }

    @Override
    public boolean isTransientUser() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isTransientUser();
    }

    @Override
    @Nonnull
    public String currentUsername() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        //Do not return a null username or this will cause a constraint violation
        return (authentication != null ? authentication.getName() : SecurityService.USER_SYSTEM);
    }

    @Override
    public UserInfo currentUser() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (authentication == null) {
            return null;
        }
        SimpleUser user = getSimpleUser(authentication);
        return user.getDescriptor();
    }

    @Override
    @Nonnull
    public UserInfo findUser(String username) {
        return findUser(username, true);
    }

    /**
     * Returns the user details for the given username.
     *
     * @param username       The unique username
     * @param errorOnAbsence throw error if user is not found
     * @return UserInfo if user with the input username exists
     */
    @Override
    @Nullable
    public UserInfo findUser(String username, boolean errorOnAbsence) {
        UserInfo user = userGroupStoreService.findUser(username);
        if (errorOnAbsence && user == null) {
            throw new UsernameNotFoundException("User " + username + " does not exist!");
        }
        return user;
    }

    @Override
    public AclInfo getAcl(String permTargetName) {
        return aclStoreService.getAcl(permTargetName);
    }

    @Override
    public AclInfo getAcl(PermissionTargetInfo permissionTarget) {
        return aclStoreService.getAcl(permissionTarget.getName());
    }

    @Override
    public boolean permissionTargetExists(String key) {
        return aclStoreService.permissionTargetExists(key);
    }

    private void cleanupAclInfo(MutableAclInfo acl) {
        acl.getMutableAces().removeIf(aceInfo -> aceInfo.getMask() == 0);
    }

    @Override
    public List<AclInfo> getAllAcls() {
        return new ArrayList<>(aclStoreService.getAllAcls());
    }

    @Override
    public Map<String, Boolean> getAllUsersAndAdminStatus(boolean justAdmins) {
        return userGroupStoreService.getAllUsersAndAdminStatus(justAdmins);
    }

    @Override
    public List<UserInfo> getAllUsers(boolean includeAdmins) {
        return getAllUsers(includeAdmins, false);
    }

    @Override
    public List<UserInfo> getAllUsers(boolean includeAdmins, boolean includingPasswords) {
        return userGroupStoreService.getAllUsers(includeAdmins, includingPasswords);
    }

    /**
     * Get all AclInfos that has permissions on given repo path
     */
    @Override
    public List<AclInfo> getRepoPathAcls(RepoPath repoPath) {
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        boolean isRemoteCache = repositoryService.localOrCachedRepoDescriptorByKey(repoPath.getRepoKey()).isCache();
        return aclStoreService.getAllAcls().stream()
                .filter(acl -> isRepoKeyinAcl(acl.getPermissionTarget().getRepoKeys(), repoPath, isRemoteCache))
                .filter(acl -> isRepoPathInAclPermissions(acl, repoPath))
                .collect(Collectors.toList());
    }

    /**
     * Filter Acl from the AclsCache that doesn't contain the repokey or any ANYs keys
     */
    private boolean isRepoKeyinAcl(List<String> repoKeys, RepoPath repoPath, boolean isRemote) {
        return repoKeys.contains(repoPath.getRepoKey()) || repoKeys.contains(ANY_REPO) ||
                (isRemote ? repoKeys.contains(ANY_REMOTE_REPO) : repoKeys.contains(ANY_LOCAL_REPO));
    }

    /**
     * Filter Acl from the AclsCache that doesn't match the acl include/exclude logic
     */
    private boolean isRepoPathInAclPermissions(AclInfo aclinfo, RepoPath repoPath) {
        return PathMatcher.matches(repoPath.getPath(), aclinfo.getPermissionTarget().getIncludes(),
                aclinfo.getPermissionTarget().getExcludes(), repoPath.isFolder());
    }

    @Override
    public org.artifactory.md.Properties findPropertiesForUser(String username) {
        return userGroupStoreService.findPropertiesForUser(username);
    }

    @Override
    public boolean addUserProperty(String username, String key, String value) {
        return userGroupStoreService.addUserProperty(username, key, value);
    }

    @Override
    public boolean updateUserProperty(String username, String key, String value) {
        boolean isUpdateSucceeded = userGroupStoreService.deleteUserProperty(username, key);
        if (isUpdateSucceeded) {
            isUpdateSucceeded = userGroupStoreService.addUserProperty(username, key, value);
        }
        return isUpdateSucceeded;
    }


    @Override
    public String getUserProperty(String username, String key) {
        return userGroupStoreService.findUserProperty(username, key);
    }

    @Override
    public void deleteUserProperty(String userName, String propertyKey) {
        userGroupStoreService.deleteUserProperty(userName, propertyKey);
    }

    @Override
    public void deletePropertyFromAllUsers(String propertyKey) {
        userGroupStoreService.deletePropertyFromAllUsers(propertyKey);
    }

    @Override
    public String getPropsToken(String userName, String propsKey) {
        return userGroupStoreService.findUserProperty(userName, propsKey);
    }

    @Override
    public boolean revokePropsToken(String userName, String propsKey) throws SQLException {
        invalidateAuthCacheEntries(userName);
        boolean deleteSucceeded = userGroupStoreService.deleteUserProperty(userName, propsKey);
        if (deleteSucceeded && ApiKeyManager.API_KEY.equals(propsKey)) {
            auditLog.revokeApiKey(userName);
        }
        return deleteSucceeded;
    }

    @Override
    public boolean createPropsToken(String userName, String propsKey, String propsValue) throws SQLException {
        boolean isPropsAddSucceeded = false;
        try {
            isPropsAddSucceeded = userGroupStoreService.addUserProperty(userName, propsKey, propsValue);
        } catch (Exception e) {
            log.debug("error adding {}:{} to db", propsKey, propsValue);
        }
        if (isPropsAddSucceeded && ApiKeyManager.API_KEY.equals(propsKey)) {
            auditLog.createApiKey(userName);
        }
        return isPropsAddSucceeded;
    }

    @Override
    public void revokeAllPropsTokens(String propsKey) throws SQLException {
        userGroupStoreService.deletePropertyFromAllUsers(propsKey);
        invalidateAuthCacheEntriesForAllUsers();
        if (ApiKeyManager.API_KEY.equals(propsKey)) {
            auditLog.revokeAllApiKeys();
        }
    }

    @Override
    public boolean updatePropsToken(String userName, String propsKey, String propsValue) throws SQLException {
        boolean isUpdateSucceeded = userGroupStoreService.deleteUserProperty(userName, propsKey);
        if (isUpdateSucceeded) {
            isUpdateSucceeded = userGroupStoreService.addUserProperty(userName, propsKey, propsValue);
            invalidateAuthCacheEntries(userName);
        }
        if (isUpdateSucceeded && ApiKeyManager.API_KEY.equals(propsKey)) {
            auditLog.updateApiKey(userName);
        }
        return isUpdateSucceeded;
    }

    /**
     * Locks user upon incorrect login attempt
     */
    @Override
    public void lockUser(@Nonnull String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Locking user {} due to incorrect login attempts", userName);
            userGroupStoreService.lockUser(userName);
            userLockInMemoryService.lockUser(userName);
            auditLog.lockUser(userName);
        }
    }

    /**
     * Unlocks locked in user
     */
    @Override
    public void unlockUser(@Nonnull String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Unlocking user {}", userName);
            userLockInMemoryService.unlockUser(userName);
            userGroupStoreService.unlockUser(userName);
            unknownUsersCache.invalidate(userName);
            resetUserIncorrectLoginAttempt(userName);
            auditLog.unlockUser(userName);
        }
    }

    /**
     * Unlocks all locked in users
     */
    @Override
    public void unlockAllUsers() {
        log.debug("Unlocking all users");
        userGroupStoreService.unlockAllUsers();
        userLockInMemoryService.unlockAllUsers();
        unknownUsersCache.invalidateAll();
        getAllUsers(true).forEach(user -> resetUserIncorrectLoginAttempt(user.getUsername()));
        auditLog.unlockAllUsers();
    }

    private void unlockAdminUsers() {
        log.debug("Unlocking all admin users");
        userGroupStoreService.unlockAdminUsers();
        auditLog.unlockAllAdminUsers();
    }

    /**
     * Registers incorrect login attempt
     */
    @Override
    public void registerIncorrectLoginAttempt(@Nonnull String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Registering incorrect login attempt for user {}", userName);
            userLockInMemoryService.registerIncorrectLoginAttempt(userName);
        }
    }

    /**
     * @return List of locked in users
     */
    @Override
    public Set<String> getLockedUsers() {
        return userGroupStoreService.getLockedUsers();
    }

    @Override
    public void encryptDecryptUserProps(String propKey, boolean encrypt) {
        userGroupStoreService.encryptDecryptUserProps(propKey, encrypt);
    }

    @Override
    public Multimap<String, String> getAllUsersInGroups() {
        return userGroupStoreService.getAllUsersInGroups();
    }

    @Override
    public boolean adminUserExists() {
        return userGroupStoreService.adminUserExists();
    }

    private void resetUserIncorrectLoginAttempt(String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Resetting incorrect login attempt for user {}", userName);
            userLockInMemoryService.resetIncorrectLoginAttempts(userName);
        }
    }

    /**
     * Triggered when user success to login
     *
     * @param userName user to intercept
     */
    @Override
    public void interceptLoginSuccess(@Nonnull String userName) {
        resetUserIncorrectLoginAttempt(userName);
    }

    /**
     * Resets incorrect login attempts
     */
    @Override
    public void resetIncorrectLoginAttempts(@Nonnull String userName) {
        log.debug("Resetting incorrect login attempts for '{}'", userName);
        userLockInMemoryService.resetIncorrectLoginAttempts(userName);
    }

    /**
     * Triggered when user fails to login and
     * locks it if amount of login failures exceeds
     * {@see LockPolicy#loginAttempts}
     *
     * @param userName   user to intercept
     * @param accessTime session creation time
     */
    @Override
    public void interceptLoginFailure(@Nonnull String userName, long accessTime) {
        if (!isAnonymousUser(userName)) {
            log.debug("Registering login attempt failure for user {}", userName);
            registerIncorrectLoginAttempt(userName);
            UserInfo user = userGroupStoreService.findUser(userName);
            if (user == null) {
                interceptUnknownUserLoginFailure(userName, accessTime);
            } else {
                interceptKnownUserLoginFailure(user);
            }
        }
    }

    /**
     * Intercepts login failure for (known to artifactory) user
     *
     * @param user user to intercept failure for
     */
    private void interceptKnownUserLoginFailure(UserInfo user) {
        log.debug("Number of incorrect login attempts: " +
                userLockInMemoryService.getIncorrectLoginAttempts(user.getUsername()) +
                ", Max allowed login attempts: " + getAllowedMaxLoginAttempts());
        if (isUserLockPolicyEnabled() && !user.isLocked() &&
                userLockInMemoryService.getIncorrectLoginAttempts(user.getUsername()) >= getAllowedMaxLoginAttempts()) {
            lockUser(user.getUsername());
        }
    }

    /**
     * Intercepts login failure for (unknown to artifactory) user
     *
     * @param userName   user to intercept failure for
     * @param accessTime access time
     */
    private void interceptUnknownUserLoginFailure(String userName, long accessTime) {
        log.trace("Memorizing {} (not a user) for blocking", userName);
        if (!isUserLocked(userName)) {
            List<Long> incorrectLoginAttempts = unknownUsersCache.getIfPresent(userName);
            if (incorrectLoginAttempts == null) {
                registerUnknownUser(userName);
                // memorize incorrect login attempt
                unknownUsersCache.put(userName, Lists.newArrayList(accessTime));
            } else {
                incorrectLoginAttempts.add(accessTime);
                if (isUserLockPolicyEnabled() &&
                        incorrectLoginAttempts.size() >= getAllowedMaxLoginAttempts()) {
                    lockUser(userName);
                    unknownUsersCache.invalidate(userName); // no need to track this user as it got locked
                }
            }
        }
    }

    /**
     * Registers unknown user in cache
     */
    private void registerUnknownUser(String userName) {
        if (!isAnonymousUser(userName)) {
            log.trace("Registering incorrect login attempt for unknown user {}", userName);
            unknownUsersCache.put(userName, new ArrayList<>(getAllowedMaxLoginAttempts()));
        }
    }

    /**
     * @return whether {@link UserLockPolicy} is enabled
     */
    @Override
    public boolean isUserLockPolicyEnabled() {
        UserLockPolicy userLockPolicy = centralConfig.getDescriptor().getSecurity().getUserLockPolicy();
        return userLockPolicy.isEnabled();
    }

    /**
     * @return whether {@link PasswordExpirationPolicy} is enabled
     */
    @Override
    public boolean isPasswordExpirationPolicyEnabled() {
        if (centralConfig.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy() != null) {
            return centralConfig.getMutableDescriptor()
                    .getSecurity()
                    .getPasswordSettings()
                    .getExpirationPolicy()
                    .isEnabled();
        }
        return false;
    }

    /**
     * @return MaxLoginAttempts allowed before user gets locked out
     */
    private int getAllowedMaxLoginAttempts() {
        UserLockPolicy userLockPolicy =
                centralConfig.getMutableDescriptor()
                        .getSecurity().getUserLockPolicy();

        return userLockPolicy.getLoginAttempts();
    }

    /**
     * @return Number of days for password to get expired
     */
    private int getPasswordExpirationDays() {
        PasswordExpirationPolicy passwordExpirationPolicy =
                centralConfig.getMutableDescriptor()
                        .getSecurity().getPasswordSettings().getExpirationPolicy();
        return passwordExpirationPolicy.getPasswordMaxAge();
    }

    /**
     * @return Max number of attempts for requesting password reset
     */
    private PasswordResetPolicy getPasswordResetPolicy(CentralConfigDescriptor centralConfig) {
        return Optional.ofNullable(centralConfig)
                .map(CentralConfigDescriptor::getSecurity)
                .map(SecurityDescriptor::getPasswordSettings)
                .map(PasswordSettings::getResetPolicy)
                .orElseGet(PasswordResetPolicy::new);
    }

    /**
     * Throws LockedException if user is locked
     */
    @Override
    public void ensureUserIsNotLocked(@Nonnull String userName) throws UserLockedException {
        log.debug("Checking if user {} is not locked", userName);
        if (!isAnonymousUser(userName) &&
                isUserLockPolicyEnabled() && isUserLocked(userName)) {
            log.debug("User {} is locked, denying login", userName);
            throw UserLockedException.userLocked(userName);
        }
    }

    /**
     * Throws LockedException if user is locked
     */
    @Override
    public void ensureSessionIsNotLocked(@Nonnull String sessionIdentifier) throws UserLockedException {
        log.debug("Checking if session {} is not locked", sessionIdentifier);
        if (isUserLockPolicyEnabled() && isUserLocked(sessionIdentifier)) {
            log.debug("Session {} is locked, denying login", sessionIdentifier);
            throw UserLockedException.sessionLocked();
        }
    }

    /**
     * Checks whether given user is locked
     * <p>
     * note: this method using caching in sake
     * of DB load preventing
     *
     * @return boolean
     */
    @Override
    public boolean isUserLocked(String userName) {
        return !isAnonymousUser(userName) &&
                userGroupStoreService.isUserLocked(userName);
    }

    /**
     * Throws LoginDelayedException if user has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     */
    @Override
    public void ensureLoginShouldNotBeDelayed(@Nonnull String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Ensuring that user {} should not be blocked", userName);
            long nextLogin = userLockInMemoryService.getNextLoginDelay(userName);
            List<Long> list = unknownUsersCache.getIfPresent(userName);
            if (nextLogin < 0 && list != null) {
                // check frontend cache for unknown users
                nextLogin = userLockInMemoryService.getNextLoginDelay(list.size(), list.get(list.size() - 1));
            }

            if (nextLogin > 0 && nextLogin > System.currentTimeMillis()) {
                log.debug("User is blocked due to incorrect login attempts till {}", nextLogin);
                throw LoginDisabledException.userLocked(userName, nextLogin);
            }
        }
    }

    /**
     * Throws LoginDelayedException if session has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     */
    @Override
    public void ensureSessionShouldNotBeDelayed(@Nonnull String sessionIdentifier) {
        if (!isAnonymousUser(sessionIdentifier)) {
            log.debug("Ensuring that user {} should not be blocked", sessionIdentifier);
            // Try to calculate next login time for known user
            long nextLogin = userLockInMemoryService.getNextLoginDelay(sessionIdentifier);
            // If user is unknown calculate next login for unknown user
            List<Long> list = unknownUsersCache.getIfPresent(sessionIdentifier);
            if (nextLogin < 0 && list != null) {
                // check frontend cache for unknown users
                nextLogin = userLockInMemoryService.getNextLoginDelay(list.size(), list.get(list.size() - 1));
            }
            if (nextLogin > 0 && nextLogin > System.currentTimeMillis()) {
                log.debug("Session is blocked due to incorrect login attempts till {}", nextLogin);
                throw LoginDisabledException.sessionLocked(sessionIdentifier, nextLogin);
            }
        }
    }

    /**
     * Performs check whther given user is anonymous
     *
     * @return true/false
     */
    private boolean isAnonymousUser(String userName) {
        return userName != null &&
                userName.length() == UserInfo.ANONYMOUS.length() &&
                UserInfo.ANONYMOUS.equals(userName);
    }

    @Override
    public boolean createUser(MutableUserInfo user) {
        user.setUsername(user.getUsername().toLowerCase());
        boolean userCreated = userGroupStoreService.createUser(user);
        if (userCreated) {
            interceptors.onUserAdd(user.getUsername());
        }
        return userCreated;
    }

    @Override
    public boolean createUserWithNoUIAccess(MutableUserInfo user) {
        //TODO [by dan]: Due to lack of time and requirement blocking ui access for a user can be done only from
        //TODO: the ui for now and we add the property here (ugly) --> disableUiAccess should be migrated to the UserInfo instead.
        if (createUser(user)) {
            userGroupStoreService.addUserProperty(user.getUsername(), UI_VIEW_BLOCKED_USER_PROP, "true");
            return true;
        }
        return false;
    }

    /**
     * Changes user password
     *
     * @param userName     user name
     * @param oldPassword  old password
     * @param newPassword1 new password
     * @param newPassword2 replication of new password
     */
    @Override
    public void changePassword(String userName, String oldPassword, String newPassword1, String newPassword2)
            throws PasswordChangeException {
        try {
            UserInfo user = findUser(userName);

            // perform user account validity check
            ensureUserIsNotLocked(userName);
            ensureLoginShouldNotBeDelayed(userName);

            if (isOldPasswordValid(findUser(userName), oldPassword, newPassword1, newPassword2)) {
                changePasswordWithoutValidation(user, newPassword1);
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordChangeException(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordChangeException("Changing password has failed, see logs for more details");
        }
    }

    /**
     * Changes user password without validating old password
     *
     * @param user user to change password to
     * @param newPassword new password
     */
    @Override
    public void changePasswordWithoutValidation(@Nonnull UserInfo user, String newPassword)
            throws PasswordChangeException {
        try {
            userGroupStoreService.changePassword(user, generateSaltedPassword(newPassword), newPassword);
            invalidateAuthCacheEntries(user.getUsername());
            dockerTokenManager.revokeToken(user.getUsername());
            log.info("Password for user '" + user.getUsername() + "' has been successfully changed");
            auditLog.userPasswordChanged(user.getUsername());
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordChangeException("", e);
        }
    }

    /**
     * Checks whether old password is valid
     *
     * @param user         a user to getAndCheckAllUserAcls password details for
     * @param oldPassword  old password
     * @param newPassword1 new password
     * @param newPassword2 replication of new password
     * @return true/false
     */
    private boolean isOldPasswordValid(UserInfo user, String oldPassword, String newPassword1, String newPassword2) {
        if (!userPassAuthenticationProvider.canUserLogin(user.getUsername(), oldPassword)) {
            throw new PasswordChangeException("Old password is incorrect");
        }

        if ((newPassword1 != null && !newPassword1.equals(newPassword2)) ||
                (newPassword2 != null && !newPassword2.equals(newPassword1))) {
            throw new PasswordChangeException("New passwords do not match");
        }

        if (oldPassword != null && oldPassword.equals(newPassword1)) {
            throw new PasswordChangeException("New password has to be different from the old one");
        }

        if (newPassword1 == null) {
            throw new PasswordChangeException("New passwords cannot be empty");
        }
        return true;
    }

    @Override
    public void updateUser(MutableUserInfo user, boolean activateListeners) {
        updateUser(user, activateListeners, true);
    }

    @Override
    public void updateUser(MutableUserInfo user, boolean activateListeners, boolean encryptProperties) {
        String userName = user.getUsername().toLowerCase();
        user.setUsername(userName);

        userGroupStoreService.updateUser(user, encryptProperties);
        if (StringUtils.isNotEmpty(user.getPassword())) {
            auditLog.userPasswordChanged(userName);
        } else {
            auditLog.userUpdated(user.getUsername());
        }
        if (activateListeners) {
            invalidateAuthCacheEntries(user.getUsername());
            dockerTokenManager.revokeToken(user.getUsername());
        }
    }

    @Override
    public void deleteUser(String username) {
        aclStoreService.removeAllUserAces(username);
        userGroupStoreService.deleteUser(username);
        interceptors.onUserDelete(username);
        invalidateAuthCacheEntries(username);
        dockerTokenManager.revokeToken(username);
    }

    /**
     * Removes the user's cache entries from the non-ui and Docker auth caches
     *
     * @param userName user to remove
     */
    private void invalidateAuthCacheEntries(String userName) {
        //Currently the only listener is AccessFilter - take care if more are added
        for (SecurityListener listener : securityListeners) {
            listener.onUserUpdate(userName);
        }
        //Also invalidate docker auth cache entries
        ContextHelper.get().beanForType(ArtifactoryTokenProvider.class).invalidateUserCacheEntries(userName);
    }

    /**
     * Removes cache entries from the non-ui and Docker auth caches for all users
     */
    private void invalidateAuthCacheEntriesForAllUsers() {
        //Clear the entire non-ui cache
        clearSecurityListeners();
        ContextHelper.get().beanForType(ArtifactoryTokenProvider.class).invalidateCacheEntriesForAllUsers();
    }

    @Override
    public void updateGroup(MutableGroupInfo groupInfo) {
        userGroupStoreService.updateGroup(groupInfo);
    }

    @Override
    public boolean createGroup(MutableGroupInfo groupInfo) {
        boolean groupCreated = userGroupStoreService.createGroup(groupInfo);
        if (groupCreated) {
            interceptors.onGroupAdd(groupInfo.getGroupName());
        }
        return groupCreated;
    }

    @Override
    public void updateGroupUsers(MutableGroupInfo group, List<String> usersInGroup) {
        // remove users from groups
        removePrevGroupUsers(group);
        // add users to group
        addUserToGroup(usersInGroup, group.getGroupName());
    }

    @Override
    public void deleteGroup(String groupName) {
        aclStoreService.removeAllGroupAces(groupName);
        if (userGroupStoreService.deleteGroup(groupName)) {
            interceptors.onGroupDelete(groupName);
        }
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        return userGroupStoreService.getAllGroups();
    }

    @Override
    public List<String> getAllAdminGroupsNames() {
        return userGroupStoreService.getAllAdminGroupsNames();
    }

    @Override
    public List<GroupInfo> getNewUserDefaultGroups() {
        return userGroupStoreService.getNewUserDefaultGroups();
    }

    @Override
    public List<GroupInfo> getAllExternalGroups() {
        return userGroupStoreService.getAllExternalGroups();
    }

    @Override
    public List<GroupInfo> getInternalGroups() {
        return userGroupStoreService.getInternalGroups();
    }

    @Override
    public Set<String> getNewUserDefaultGroupsNames() {
        return userGroupStoreService.getNewUserDefaultGroupsNames();
    }

    @Override
    public void addUsersToGroup(String groupName, List<String> usernames) {
        userGroupStoreService.addUsersToGroup(groupName, usernames);
        interceptors.onAddUsersToGroup(groupName, usernames);
        for (String username : usernames) {
            invalidateAuthCacheEntries(username);
            dockerTokenManager.revokeToken(username);
        }
    }

    @Override
    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        userGroupStoreService.removeUsersFromGroup(groupName, usernames);
        interceptors.onRemoveUsersFromGroup(groupName, usernames);
        for (String username : usernames) {
            invalidateAuthCacheEntries(username);
            dockerTokenManager.revokeToken(username);
        }
    }

    @Override
    public List<String> findUsersInGroup(String groupName) {
        return userGroupStoreService.findUsersInGroup(groupName);
    }

    @Override
    public String resetPassword(String userName, String remoteAddress, String resetPageUrl) {
        validateResetPasswordAttempt(remoteAddress);
        UserInfo userInfo = null;
        try {
            userInfo = findUser(userName);
        } catch (UsernameNotFoundException e) {
            //Alert in the log when trying to reset a password of an unknown user
            log.warn("An attempt has been made to reset a password of unknown user: {}", userName);
        }

        //If the user is found, and has an email address
        if (userInfo != null && !StringUtils.isEmpty(userInfo.getEmail())) {

            //If the user hasn't got sufficient permissions
            if (!userInfo.isUpdatableProfile()) {
                throw new RuntimeException("The specified user is not permitted to reset his password.");
            }

            //Generate and send a password reset key
            try {
                generatePasswordResetKey(userName, remoteAddress, resetPageUrl);
            } catch (EmailException ex) {
                String message = ex.getMessage() + " Please contact your administrator.";
                throw new RuntimeException(message);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        } else if (userInfo != null) {
            log.warn("Reset password e-mail was not really sent, e-mail not set for user: {}", userName);
        }
        return "We have sent you via email a link for resetting your password. Please check your inbox.";
    }

    /**
     * Validates a reset password attempt from the given remote address.
     *
     * @throws ResetPasswordException on an invalid attempt (e.g. too many/frequent attempts)
     */
    void validateResetPasswordAttempt(String remoteAddress) {
        PasswordResetPolicy policy = getPasswordResetPolicy(centralConfig.getDescriptor());
        if (policy.isEnabled()) {
            try {
                List<Long> attempts = resetPasswordAttemptsBySourceCache.get(remoteAddress, Lists::newArrayList);
                if (!attempts.isEmpty()) {
                    if (attempts.size() >= policy.getMaxAttemptsPerAddress()) {
                        throw ResetPasswordException.tooManyAttempts(remoteAddress);
                    }
                    if (System.currentTimeMillis() - attempts.get(attempts.size() - 1) <
                            MIN_DELAY_BETWEEN_FORGOT_PASSWORD_ATTEMPTS_PER_SOURCE) {
                        throw ResetPasswordException.tooFrequentAttempts(remoteAddress);
                    }
                }
                attempts.add(System.currentTimeMillis());
            } catch (ExecutionException e) {
                String errorMessage =
                        "Unexpected error while verifying legitimacy of password reset requested from remote address " +
                                remoteAddress;
                log.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }
    }

    @Override
    public UserInfo findOrCreateExternalAuthUser(String userName, boolean transientUser) {
        return findOrCreateExternalUser(userName, transientUser, false);
    }

    @Override
    public UserInfo findOrCreateExternalAuthUser(String userName, boolean transientUser, boolean updateProfile) {
        return findOrCreateExternalUser(userName, transientUser, updateProfile);
    }

    /**
     * find or create external user in db
     *
     * @param userName      - user name to find
     * @param transientUser - if true ,  user mark as transient (not created in db)
     * @param updateProfile - if true ,  user will be able to update it own profile
     * @return {@link UserInfo}
     */
    private UserInfo findOrCreateExternalUser(String userName, boolean transientUser, boolean updateProfile) {
        UserInfo userInfo;
        try {
            userInfo = findUser(userName.toLowerCase());
        } catch (UsernameNotFoundException nfe) {
            try {
                userInfo = autoCreateUser(userName, transientUser, updateProfile);
            } catch (ValidationException ve) {
                log.error("Auto-Creation of '" + userName + "' has failed, " + ve.getMessage());
                throw new InvalidNameException(userName, ve.getMessage(), ve.getIndex());
            }
        }
        return userInfo;
    }


    /**
     * remove group users before update
     *
     * @param group - group data
     */
    private void removePrevGroupUsers(MutableGroupInfo group) {
        List<String> usersInGroup = findUsersInGroup(group.getGroupName());
        if (usersInGroup != null && !usersInGroup.isEmpty()) {
            removeUsersFromGroup(group.getGroupName(), usersInGroup);
        }
    }

    /**
     * @param users     - user list to be added to group
     * @param groupName - group name
     */
    private void addUserToGroup(List<String> users, String groupName) {
        if (users != null && !users.isEmpty()) {
            addUsersToGroup(groupName, users);
        }
    }

    /**
     * Auto create user
     *
     * @return {@link UserInfo}
     *
     * @throws ValidationException if userName is invalid
     */
    private UserInfo autoCreateUser(String userName, boolean transientUser,
            boolean updateProfile) throws ValidationException {
        UserInfo userInfo;
        log.debug("Creating new external user '{}'", userName);

        // make sure username answer artifactory standards RTFACT-8259
        NameValidator.validate(userName);

        UserInfoBuilder userInfoBuilder = new UserInfoBuilder(userName.toLowerCase()).updatableProfile(updateProfile);
        userInfoBuilder.internalGroups(getNewUserDefaultGroupsNames());
        if (transientUser) {
            userInfoBuilder.transientUser();
        }
        userInfo = userInfoBuilder.build();

        // Save non transient user
        if (!transientUser) {
            auditLog.externalUserCreated(userName);
            boolean success = userGroupStoreService.createUser(userInfo);
            if (!success) {
                log.error("User '{}' was not created!", userInfo);
            }
        }
        return userInfo;
    }

    @Override
    @Nullable
    public GroupInfo findGroup(String groupName) {
        return userGroupStoreService.findGroup(groupName);
    }

    @Override
    public String createEncryptedPasswordIfNeeded(UserInfo user, String password) {
        if (isPasswordEncryptionEnabled() && StringUtils.isNotBlank(password)) {
            // Checking if the password sent by user is an API key. If so we don't encrypt it, just return it.
            if (EncodingType.ARTIFACTORY_API_KEY.isEncodedByMe(password)) {
                log.debug("Returning API Key as encrypted password.");
                return password;
            }
            EncryptionWrapper masterWrapper = ArtifactoryHome.get().getArtifactoryEncryptionWrapper();
            EncodedKeyPair encodedKeyPair;
            EncryptionWrapper encryptionWrapper;
            try {
                if (StringUtils.isBlank(user.getPrivateKey())) {
                    encodedKeyPair = createKeyPairForUser(user, masterWrapper);
                } else {
                    encodedKeyPair = convertOldKeyFormatIfNeeded(user, masterWrapper);
                }
                encryptionWrapper = EncryptionWrapperFactory.createKeyWrapper(masterWrapper, encodedKeyPair);
                return encryptionWrapper.encryptIfNeeded(password);
            } catch (Exception e) {
                String err = "Error inferring keypair for user " + user.getUsername() + ": " + e.getMessage()
                        + " generating a new keypair if called from profile page.";
                if (log.isDebugEnabled()) {
                    log.error(err, e);
                } else {
                    log.error(err);
                }
                //Throw PasswordEncryptionFailureException. The UnlockUserProfileService#updateUserInfo should fallback
                //and re-invoke the password
                throw new PasswordEncryptionFailureException("Failed to encrypt password. " +
                        "To retrieve a new encrypted password unlock your user profile page again.");
            }
        }
        return password;
    }

    /**
     * Creates and saves a new keypair for {@param user} using {@param masterWrapper}
     */
    private EncodedKeyPair createKeyPairForUser(UserInfo user, EncryptionWrapper masterWrapper) {
        EncodedKeyPair encodedKeyPair;
        log.info("Creating keys for user '" + user.getUsername() + "'");
        MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
        DecodedKeyPair decodedKeyPair = new DecodedKeyPair(JFrogCryptoHelper.generateKeyPair());
        encodedKeyPair = new EncodedKeyPair(decodedKeyPair, masterWrapper);
        saveEncodedKeyPairInUser(mutableUser, encodedKeyPair);
        return encodedKeyPair;
    }

    /**
     * Converts old format keypair to new format for {@param user} using {@param masterWrapper} if required,
     * May generate a new keypair if old format encoding is used which we don't support any more.
     */
    private EncodedKeyPair convertOldKeyFormatIfNeeded(UserInfo user, EncryptionWrapper masterWrapper) {
        EncodedKeyPair encodedKeyPair;
        encodedKeyPair = new EncodedKeyPair(user.getPrivateKey(), user.getPublicKey());
        EncodedKeyPair toSaveEncodedKeyPair = encodedKeyPair.toSaveEncodedKeyPair(masterWrapper);
        if (toSaveEncodedKeyPair != null) {
            log.info("Reformatting keys for user '" + user.getUsername() + "'");
            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
            saveEncodedKeyPairInUser(mutableUser, toSaveEncodedKeyPair);
            encodedKeyPair = toSaveEncodedKeyPair;
        }
        return encodedKeyPair;
    }

    private void saveEncodedKeyPairInUser(MutableUserInfo mutableUser, EncodedKeyPair encodedKeyPair) {
        mutableUser.setPrivateKey(encodedKeyPair.getEncodedPrivateKey());
        mutableUser.setPublicKey(encodedKeyPair.getEncodedPublicKey());
        updateUser(mutableUser, false);
    }

    /**
     * Generates a password recovery key for the specified user and send it by mail
     *
     * @param username      User to rest his password
     * @param remoteAddress The IP of the client that sent the request
     * @param resetPageUrl  The URL to the password reset page
     */
    @Override
    public void generatePasswordResetKey(String username, String remoteAddress, String resetPageUrl) throws Exception {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }

        //If user has valid email
        if (!StringUtils.isEmpty(userInfo.getEmail())) {
            if (!userInfo.isUpdatableProfile()) {
                //If user is not allowed to update his profile
                throw new AuthorizationException("User is not permitted to reset his password.");
            }

            //Build key by UUID + current time millis + client ip -> encoded in B64
            UUID uuid = UUID.randomUUID();
            String passwordKey = uuid.toString() + ":" + System.currentTimeMillis() + ":" + remoteAddress;
            byte[] encodedKey = Base64.encodeBase64URLSafe(passwordKey.getBytes(Charsets.UTF_8));
            String encodedKeyString = new String(encodedKey, Charsets.UTF_8);

            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
            mutableUser.setGenPasswordKey(encodedKeyString);
            updateUser(mutableUser, false);

            //Add encoded key to page url
            String resetPage = resetPageUrl + "?key=" + encodedKeyString;

            //If there are any admins with valid email addresses, add them to the list that the message will contain
            //String adminList = getAdminListBlock(userInfo);
            InputStream stream = null;
            try {
                //Get message body from properties and substitute variables
                stream = getClass().getResourceAsStream("/org/artifactory/email/messages/resetPassword.properties");
                ResourceBundle resourceBundle = new PropertyResourceBundle(stream);
                String body = resourceBundle.getString("body");
                body = MessageFormat.format(body, username, remoteAddress, resetPage);
                mailService.sendMail(new String[]{userInfo.getEmail()}, "Reset password request", body);
            } catch (EmailException e) {
                log.error("Error while resetting password for user: '" + username + "'.", e);
                throw e;
            } finally {
                IOUtils.closeQuietly(stream);
            }
            log.info("The user: '{}' has been sent a password reset message by mail.", username);
        }
    }

    @Override
    public SerializablePair<Date, String> getPasswordResetKeyInfo(String username) {
        UserInfo userInfo = findUser(username);
        String passwordKey = userInfo.getGenPasswordKey();
        if (StringUtils.isEmpty(passwordKey)) {
            return null;
        }

        byte[] decodedKey = Base64.decodeBase64(passwordKey.getBytes(Charsets.UTF_8));
        String decodedKeyString = new String(decodedKey, Charsets.UTF_8);
        String[] splitKey = decodedKeyString.split(":");

        //Key must be in 3 parts
        if (splitKey.length < 3) {
            throw new IllegalArgumentException("Password reset key must contain 3 parts - 'UUID:Date:IP'");
        }

        String time = splitKey[1];
        String ip = splitKey[2];

        Date date = new Date(Long.parseLong(time));

        return new SerializablePair<>(date, ip);
    }

    @Override
    public SerializablePair<String, Long> getUserLastLoginInfo(String username) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user (might be transient user)
            log.trace("Could not retrieve last login info for username '{}'.", username);
            return null;
        }

        SerializablePair<String, Long> pair = null;
        String lastLoginClientIp = userInfo.getLastLoginClientIp();
        long lastLoginTimeMillis = userInfo.getLastLoginTimeMillis();
        if (!StringUtils.isEmpty(lastLoginClientIp) && (lastLoginTimeMillis != 0)) {
            pair = new SerializablePair<>(lastLoginClientIp, lastLoginTimeMillis);
        }
        return pair;
    }

    @Override
    public boolean isHasPriorLogin() {
        return userGroupStoreService.getAllUsers(true, false)
                .stream()
                .anyMatch(user -> user.getLastLoginTimeMillis() != 0);
    }

    @Override
    public void updateUserLastLogin(String username, String clientIp, long loginTimeMillis) {
        long lastLoginBufferTimeSecs = ConstantValues.userLastAccessUpdatesResolutionSecs.getLong();
        if (lastLoginBufferTimeSecs < 1) {
            log.debug("Skipping the update of the last login time for the user '{}': tracking is disabled.", username);
            return;
        }
        long lastLoginBufferTimeMillis = TimeUnit.SECONDS.toMillis(lastLoginBufferTimeSecs);
        UserInfo userInfo = userGroupStoreService.findUser(username);
        if (userInfo == null) {
            // user not found (might be a transient user)
            log.trace("Could not update non-exiting username: {}'.", username);
            return;
        }
        long timeSinceLastLogin = loginTimeMillis - userInfo.getLastLoginTimeMillis();
        if (timeSinceLastLogin < lastLoginBufferTimeMillis) {
            log.debug("Skipping the update of the last login time for the user '{}': " +
                    "was updated less than {} seconds ago.", username, lastLoginBufferTimeSecs);
            return;
        }
        MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
        mutableUser.setLastLoginTimeMillis(loginTimeMillis);
        mutableUser.setLastLoginClientIp(clientIp);
        updateUser(mutableUser, false);
    }

    /**
     * Updates user last access time, if user is not exist in artifactory
     * keeps track of it in volatile cache
     *
     * @param userName         Name of user that performed an action
     * @param clientIp         The IP of the client that has accessed
     * @param accessTimeMillis The time of access
     */
    @Override
    public void updateUserLastAccess(String userName, String clientIp, long accessTimeMillis) {
        log.debug("Updating access details for user {}, time={}, ip={}", userName, accessTimeMillis, clientIp);
        userLockInMemoryService.updateUserAccess(userName, isUserLockPolicyEnabled(), accessTimeMillis);
    }

    @Override
    public boolean isHttpSsoProxied() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isHttpSsoProxied();
    }

    @Override
    public boolean isNoHttpSsoAutoUserCreation() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isNoAutoUserCreation();
    }

    @Override
    public String getHttpSsoRemoteUserRequestVariable() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        if (httpSsoSettings == null) {
            return null;
        } else {
            return httpSsoSettings.getRemoteUserRequestVariable();
        }
    }

    @Override
    public boolean hasPermission(ArtifactoryPermission artifactoryPermission) {
        return isAdmin() || !getPermissionTargets(artifactoryPermission).isEmpty();
    }

    @Override
    public boolean canRead(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canAnnotate(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDeploy(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canDelete(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canManage(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canManage(PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canRead(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.READ, new SimpleUser(user));
    }

    @Override
    public boolean canAnnotate(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.ANNOTATE, new SimpleUser(user));
    }

    @Override
    public boolean canDeploy(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.DEPLOY, new SimpleUser(user));
    }

    @Override
    public boolean canDelete(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.DELETE, new SimpleUser(user));
    }

    @Override
    public boolean canManage(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.MANAGE, new SimpleUser(user));
    }

    @Override
    public boolean canRead(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canAnnotate(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDelete(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canDeploy(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canManage(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canRead(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canAnnotate(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDelete(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canDeploy(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canManage(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.MANAGE);
    }

    @Override
    public Map<PermissionTargetInfo, AceInfo> getUserPermissionByPrincipal(String username) {
        Map<PermissionTargetInfo, AceInfo> aceInfoMap = Maps.newHashMap();
        UserInfo user = userGroupStoreService.findUser(username);
        if (user == null) {
            return Maps.newHashMap();
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(new SimpleUser(user));
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            addSidsPermissions(aceInfoMap, sids, acl);
        }
        return aceInfoMap;
    }

    @Override
    public Multimap<PermissionTargetInfo, AceInfo> getGroupsPermissions(List<String> groups) {
        Multimap<PermissionTargetInfo, AceInfo> aceInfoMap = HashMultimap.create();
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            for (AceInfo ace : acl.getAces()) {
                if (ace.isGroup() && groups.contains(ace.getPrincipal())) {
                    aceInfoMap.put(acl.getPermissionTarget(), ace);
                }
            }
        }
        return aceInfoMap;
    }


    @Override
    public Map<PermissionTargetInfo, AceInfo> getUserPermissions(String userName) {
        Map<PermissionTargetInfo, AceInfo> aceInfoMap = Maps.newHashMap();
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            acl.getAces()
                    .stream()
                    .filter(ace -> !ace.isGroup())
                    .filter(ace -> userName.equals(ace.getPrincipal()))
                    .forEach(ace -> aceInfoMap.put(acl.getPermissionTarget(), ace));
        }
        return aceInfoMap;
    }


    /**
     * add artifactory sids permissions to map
     *
     * @param aceInfoMap - permission target and principal info map
     * @param sids       -permissions related sids
     * @param acl        - permissions acls
     */
    private void addSidsPermissions(Map<PermissionTargetInfo, AceInfo> aceInfoMap, Set<ArtifactorySid> sids,
            AclInfo acl) {
        //Check that we match the sids
        acl.getAces()
                .stream()
                .filter(ace -> sids.contains(new ArtifactorySid(ace.getPrincipal(), ace.isGroup())))
                .forEach(ace -> aceInfoMap.put(acl.getPermissionTarget(), ace));
    }


    @Override
    public boolean userHasPermissionsOnRepositoryRoot(String repoKey) {
        Repo repo = repositoryService.repositoryByKey(repoKey);
        if (repo == null) {
            // Repo does not exists => No permissions
            return false;
        }
        // If it is a real (i.e local or cached simply check permission on root.
        if (repo.isReal()) {
            // If repository is real, check if the user has any permission on the root.
            if (repo instanceof RemoteRepo) {
                RepoPath remoteRepoPath = InternalRepoPathFactory.repoRootPath(repoKey);
                repoKey = InternalRepoPathFactory.cacheRepoPath(remoteRepoPath).getRepoKey();
            }
            return hasPermissionOnRoot(repoKey);
        } else {
            // If repository is virtual go over all repository associated with it and check if user has permissions
            // on it root.
            VirtualRepo virtualRepo = (VirtualRepo) repo;
            // Go over all resolved cached repos, i.e. if we have virtual repository aggregation,
            // This will give the resolved cached repos.
            Set<LocalCacheRepo> localCacheRepoList = virtualRepo.getResolvedLocalCachedRepos();
            for (LocalCacheRepo localCacheRepo : localCacheRepoList) {
                LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(localCacheRepo.getKey());
                if (localRepo != null) {
                    if (hasPermissionOnRoot(localRepo.getKey())) {
                        return true;
                    }
                }
            }
            // Go over all resolved local repositories, will bring me the resolved local repos from aggregation.
            Set<LocalRepo> repoList = virtualRepo.getResolvedLocalRepos();
            for (LocalRepo localCacheRepo : repoList) {
                LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(localCacheRepo.getKey());
                if (localRepo != null) {
                    if (hasPermissionOnRoot(localRepo.getKey())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDisableInternalPassword() {
        UserInfo simpleUser = currentUser();
        return (simpleUser == null);
    }

    @Override
    public String currentUserEncryptedPassword() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if ((authentication != null) && authentication.isAuthenticated()) {
            String authUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
            String password = (String) authentication.getCredentials();
            if (StringUtils.isNotBlank(password)) {
                UserInfo user = userGroupStoreService.findUser(authUsername);
                if (user == null) {
                    log.warn("Can't return the encrypted password of the unfound user '{}'", authUsername);
                } else {
                    return createEncryptedPasswordIfNeeded(user, password);
                }
            }
        }
        return null;
    }

    @Override
    public String currentUserEncryptedPasswordOrApiKey() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if ((authentication != null) && authentication.isAuthenticated()) {
            String authUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
            UserInfo user = userGroupStoreService.findUser(authUsername);
            if (user == null) {
                log.warn("Can't return the encrypted password of the unfound user '{}'", authUsername);
            } else if (user.isExternal()) {
                // For users without internal password (e.g. OAuth users) return the API key if exists (it's already encrypted)
                TokenKeyValue token = apiKeyManager.getToken(authUsername);
                if (token != null) {
                    return token.getToken();
                }
            } else {
                String password = (String) authentication.getCredentials();
                if (StringUtils.isNotBlank(password)) {
                    return createEncryptedPasswordIfNeeded(user, password);
                }
            }
        }
        return null;
    }

    /**
     * Getting a score per repo in the PermissionHeuristicScore range:
     * Rating is by the readability of the repo based on the current user permissions.
     * Highest is admin --> readAll --> readWithExclusion --> readNotAllowed
     * If there is read access:
     * In case repo permission target has no exclusion AND inclusion is default (ANY_PATH)  --> readAll
     * else -->  readWithExclusion
     *
     * @return Score of the repo in the PermissionHeuristicScore
     */
    @Override
    public PermissionHeuristicScore getStrongestReadPermissionTarget(String repoKey) {
        AclCache aclCache = aclStoreService.getAclCache();
        Authentication authentication = AuthenticationHelper.getAuthentication();
        SimpleUser simpleUser = getSimpleUser(authentication);
        if (simpleUser.isEffectiveAdmin()) {
            return PermissionHeuristicScore.admin;
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(simpleUser);
        PermissionHeuristicScore bestScore = PermissionHeuristicScore.readNotAllowed;
        for (ArtifactorySid sid : sids) {
            Map<String, Map<String, Set<AclInfo>>> map = getAclCacheRelevantMap(aclCache, sid);
            Map<String, Set<AclInfo>> repKeyToAclInfoMap = map.get(sid.getPrincipal());
            Set<AclInfo> aclInfoSet = populateAclInfoSetByRepo(repoKey, repKeyToAclInfoMap);
            // Check for READ permission per acl
            for (AclInfo aclInfo : aclInfoSet) {
                // If no read access, moving to the next acl
                if (!hasPermissionOnAcl(aclInfo, ArtifactoryPermission.READ, simpleUser)) {
                    continue;
                }
                PermissionTargetInfo permissionTarget = aclInfo.getPermissionTarget();
                // User has read access, now determine the score
                if (permissionTarget.getExcludes().isEmpty() && permissionTarget.getIncludes().size() == 1 &&
                        permissionTarget.getIncludes().get(0).equals(PermissionTargetInfo.ANY_PATH)) {
                    bestScore = bestScore.ordinal() < PermissionHeuristicScore.readAll.ordinal() ?
                            PermissionHeuristicScore.readAll : bestScore;
                } else {
                    bestScore = bestScore.ordinal() < PermissionHeuristicScore.readWithExclusion.ordinal() ?
                            PermissionHeuristicScore.readWithExclusion : bestScore;
                }

            }
        }
        return bestScore;
    }

    /**
     * Aggregate all aclInfo of a repo + Any_Repo + Any_Local_Repo into one set
     */
    private Set<AclInfo> populateAclInfoSetByRepo(String repoKey, Map<String, Set<AclInfo>> repKeyToAclInfoMap) {
        Set<AclInfo> aclInfoSet = new HashSet<>();
        if (repKeyToAclInfoMap != null) {
            addAllToAclSetByRepoKey(repKeyToAclInfoMap, aclInfoSet, PermissionTargetInfo.ANY_REPO);
            addAllToAclSetByRepoKey(repKeyToAclInfoMap, aclInfoSet, PermissionTargetInfo.ANY_LOCAL_REPO);
            addAllToAclSetByRepoKey(repKeyToAclInfoMap, aclInfoSet, repoKey);
        }
        return aclInfoSet;
    }

    /**
     * In case the repo AclInfoMp is not empty --> we add the relevant AclInfo from the map to the aclInfoSet
     */
    private void addAllToAclSetByRepoKey(Map<String, Set<AclInfo>> repKeyToAclInfoMap, Set<AclInfo> aclInfoSet,
            String key) {
        if (!CollectionUtils.isNullOrEmpty(repKeyToAclInfoMap.get(key))) {
            aclInfoSet.addAll(repKeyToAclInfoMap.get(key));
        }
    }

    /**
     * Checks whether the sid is per user or group and returns the relevant map from the acl cache
     */
    private Map<String, Map<String, Set<AclInfo>>> getAclCacheRelevantMap(AclCache aclCache, ArtifactorySid sid) {
        Map<String, Map<String, Set<AclInfo>>> map;
        if (sid.isGroup()) {
            map = aclCache.getGroupResultMap();
        } else {
            map = aclCache.getUserResultMap();
        }
        return map;
    }

    private boolean hasPermissionOnRoot(String repoKey) {
        RepoPath path = InternalRepoPathFactory.repoRootPath(repoKey);
        for (ArtifactoryPermission permission : ArtifactoryPermission.values()) {
            if (hasPermission(path, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(RepoPath repoPath, ArtifactoryPermission permission) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // Admins has permissions for all paths and all repositories
        if (isAdmin(authentication)) {
            return true;
        }
        // Anonymous users are checked only if anonymous access is enabled
        if (isAnonymous() && !isAnonAccessEnabled()) {
            return false;
        }
        if (TrashService.TRASH_KEY.equals(repoPath.getRepoKey()) && !isAdmin()) {
            return false;
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(getSimpleUser(authentication));
        return isGranted(repoPath, permission, sids) || (ArtifactoryPermission.READ.equals(permission) &&
                isGrantedByAccessToken(repoPath, authentication));
    }

    /**
     * If authentication is of type {@link AccessTokenAuthentication}, test it's scope tokens for read access from a
     * {@link ArtifactoryRepoPathScopeToken}
     */
    private boolean isGrantedByAccessToken(RepoPath repoPath, Authentication authentication ) {
        if (authentication instanceof AccessTokenAuthentication) {
            AccessTokenAuthentication accessTokenAuthentication = (AccessTokenAuthentication) authentication;
            String repoPathChecksum = calculateRepoPathCheckSum(repoPath);
            List<String> scope = accessTokenAuthentication.getAccessToken().getScope();
            return scope.stream().anyMatch(scopeToken -> isGrantedByAccessScopeToken(scopeToken, repoPathChecksum));
        }
        return false;
    }

    /**
     * Check if the scope token is of type {@link ArtifactoryRepoPathScopeToken}, and then compare repoPath checksum with token checksum
     */
    private boolean isGrantedByAccessScopeToken(String scopeToken, String repoPathChecksum) {
        return ArtifactoryRepoPathScopeToken.accepts(scopeToken) &&
                repoPathChecksum.equals(ArtifactoryRepoPathScopeToken.getPath(scopeToken));
    }

    private String calculateRepoPathCheckSum(RepoPath repoPath) {
        String repoPathName = repoPath.getRepoKey() + "/" + repoPath.getPath();
        return ChecksumUtils.calculateSha1(new ByteArrayInputStream(repoPathName.getBytes()));
    }

    private boolean hasPermission(SimpleUser user, RepoPath repoPath, ArtifactoryPermission permission) {
        // Admins has permissions for all paths and all repositories
        if (user.isEffectiveAdmin()) {
            return true;
        }

        // Anonymous users are checked only if anonymous access is enabled
        if (user.isAnonymous() && !isAnonAccessEnabled()) {
            return false;
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(user);
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(final GroupInfo group, RepoPath repoPath, ArtifactoryPermission permission) {
        if (group.isAdminPrivileges()) {
            return true;
        }
        Set<ArtifactorySid> sid = new HashSet<ArtifactorySid>() {{
            add(new ArtifactorySid(group.getGroupName(), true));
        }};
        return isGranted(repoPath, permission, sid);
    }

    private boolean isPermissionTargetIncludesRepoKey(String repoKey, PermissionTargetInfo permissionTarget) {
        // checks if repo key is part of the permission target repository keys taking into account
        // the special logical repo keys of a permission target like "Any", "All Local" etc.
        List<String> repoKeys = permissionTarget.getRepoKeys();
        if (repoKeys.contains(PermissionTargetInfo.ANY_REPO)) {
            return true;
        }

        if (repoKeys.contains(repoKey)) {
            return true;
        }

        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (localRepo != null) {
            if (!(localRepo instanceof DistributionRepo) && !localRepo.isCache() &&
                    repoKeys.contains(PermissionTargetInfo.ANY_LOCAL_REPO)) {
                return true;
            } else if (localRepo.isCache() && repoKeys.contains(PermissionTargetInfo.ANY_REMOTE_REPO)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermissionOnPermissionTarget(PermissionTargetInfo permTarget, ArtifactoryPermission permission) {
        AclInfo acl = aclStoreService.getAcl(permTarget.getName());
        return hasPermissionOnAcl(acl, permission);
    }

    private boolean hasPermissionOnPermissionTarget(PermissionTargetInfo permTarget, ArtifactoryPermission permission,
            SimpleUser user) {
        AclInfo acl = aclStoreService.getAcl(permTarget.getName());
        return hasPermissionOnAcl(acl, permission, user);
    }

    private boolean hasPermissionOnAcl(AclInfo acl, ArtifactoryPermission permission) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // Admins has permissions on any target, if not check if has permissions on acl
        return isAdmin(authentication) || hasPermissionOnAcl(acl, permission, getSimpleUser(authentication));

    }

    private boolean hasPermissionOnAcl(AclInfo acl, ArtifactoryPermission permission, SimpleUser user) {
        // Admins has permissions on any target, if not check if has permissions
        return user.isEffectiveAdmin() || isGranted(acl, permission, getUserEffectiveSids(user));

    }

    private boolean isGranted(AclInfo acl, ArtifactoryPermission permission, Set<ArtifactorySid> sids) {
        for (AceInfo ace : acl.getAces()) {
            //Check that we match the sids
            if (sids.contains(new ArtifactorySid(ace.getPrincipal(), ace.isGroup()))) {
                if ((ace.getMask() & permission.getMask()) > 0) {
                    //Any of the permissions is enough for granting
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGranted(AclInfo acl, ArtifactoryPermission permission, ArtifactorySid sid) {
        for (AceInfo ace : acl.getAces()) {
            if (!ace.getPrincipal().equals(sid.getPrincipal())) {
                continue;
            }
            //Check that we match the sids
            if ((ace.getMask() & permission.getMask()) > 0) {
                //Any of the permissions is enough for granting
                return true;
            }
        }
        return false;
    }


    private boolean isGranted(
            RepoPath repoPath, ArtifactoryPermission permission, Set<ArtifactorySid> sids) {
        AclCache aclCache = aclStoreService.getAclCache();
        for (ArtifactorySid sid : sids) {
            Map<String, Map<String, Set<AclInfo>>> map = getAclCacheRelevantMap(aclCache, sid);
            if (getAndCheckAllUserAcls(repoPath, permission, sid, map)) {
                return true;
            }
        }
        return false;
    }

    private boolean getAndCheckAllUserAcls(RepoPath repoPath, ArtifactoryPermission permission, ArtifactorySid sid,
            Map<String, Map<String, Set<AclInfo>>> map) {
        Map<String, Set<AclInfo>> repoSidAcls = map.get(sid.getPrincipal());
        if (repoSidAcls != null) {
            if (getAndCheckAcl(repoPath.getRepoKey(), repoSidAcls, repoPath, permission, sid)) {
                return true;
            }
            // check if user has any local repo permission
            if (getAndCheckAcl(PermissionTargetInfo.ANY_LOCAL_REPO, repoSidAcls, repoPath, permission, sid)) {
                return true;
            }
            // check if user has any remote repo permission
            if (getAndCheckAcl(PermissionTargetInfo.ANY_REMOTE_REPO, repoSidAcls, repoPath, permission, sid)) {
                return true;
            }
            if (getAndCheckAcl(PermissionTargetInfo.ANY_REPO, repoSidAcls, repoPath, permission, sid)) {
                return true;
            }
        }
        return false;
    }

    private boolean getAndCheckAcl(String checkedRepo, Map<String, Set<AclInfo>> repoSidAcls, RepoPath repoPath,
            ArtifactoryPermission permission, ArtifactorySid sid) {
        Collection<AclInfo> allItemAcls = repoSidAcls.get(checkedRepo);
        // cached remote repos can still produce null maps (RTFACT-6939). check on remote compatible repos instead.
        if (allItemAcls == null) {
            String remoteRepoKey = makeRemoteRepoKeyAclCompatible(checkedRepo);
            allItemAcls = repoSidAcls.get(remoteRepoKey);
        }
        if (allItemAcls != null) {
            if (permissionCheckOnAcl(allItemAcls, repoPath, permission, sid)) {
                return true;
            }
        }
        return false;
    }

    private boolean permissionCheckOnAcl(Collection<AclInfo> allAcls, RepoPath repoPath,
            ArtifactoryPermission permission, ArtifactorySid sid) {
        for (AclInfo acl : allAcls) {

            if (!(acl instanceof ImmutableAclInfo)) {
                RuntimeException runtimeException = new RuntimeException(
                        "Checking for permission on " + acl
                                + " should use only immutable security objects not " + acl.getClass());
                log.error(runtimeException.getMessage(), runtimeException);
            }
            String repoKey = repoPath.getRepoKey();
            String aclCompatibleRepoKey = makeRemoteRepoKeyAclCompatible(repoKey);  //acl compatible key for remotes
            String path = repoPath.getPath();
            boolean folder = repoPath.isFolder();
            PermissionTargetInfo aclPermissionTarget = acl.getPermissionTarget();
            if (isPermissionTargetIncludesRepoKey(repoKey, aclPermissionTarget)
                    || isPermissionTargetIncludesRepoKey(aclCompatibleRepoKey, aclPermissionTarget)) {
                boolean checkPartialPath = (permission.getMask() &
                        (ArtifactoryPermission.READ.getMask() | ArtifactoryPermission.DEPLOY.getMask())) != 0;
                boolean behaveAsFolder = folder && checkPartialPath;
                boolean match = matches(aclPermissionTarget, path, behaveAsFolder);
                if (match) {
                    if (isGranted(acl, permission, sid)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void exportTo(ExportSettings settings) {
        throw new UnsupportedOperationException("Logic moved to access");
    }

    /**
     * @deprecated as access should manage security data
     */
    @Deprecated
    @Override
    public SecurityInfo getSecurityData() {
        List<UserInfo> users = getAllUsers(true, true);
        List<GroupInfo> groups = getAllGroups();
        List<AclInfo> acls = getAllAcls();
        SecurityInfo descriptor = InfoFactoryHolder.get().createSecurityInfo(users, groups, acls);
        descriptor.setVersion(SecurityVersion.getCurrent().name());
        return descriptor;
    }

    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        status.status("Importing security...", log);
        importSecurityXml(settings, status);
    }

    private void importSecurityXml(ImportSettings settings, MutableStatusHolder status) {
        //Import the new security definitions
        File baseDir = settings.getBaseDir();
        // First check for security.xml file
        File securityXmlFile = new File(baseDir, FILE_NAME);
        if (!securityXmlFile.exists()) {
            log.info("Security file {} does not exist no import of security will" +
                    " be done (access import should handle security for new imports)", securityXmlFile);
            return;
        }
        SecurityInfo securityInfo;
        try {
            securityInfo = new SecurityInfoReader().read(securityXmlFile);
        } catch (Exception e) {
            status.warn("Could not read security file", log);
            return;
        }
        SecurityService me = InternalContextHelper.get().beanForType(SecurityService.class);
        me.importSecurityData(securityInfo);
    }

    @Override
    public void createDefaultAdminUser() {
        log.info("Creating the default super user '" + DEFAULT_ADMIN_USER + "', since no admin user exists!");
        UserInfo defaultAdmin = userGroupStoreService.findUser(DEFAULT_ADMIN_USER);

        UserInfoBuilder builder = new UserInfoBuilder(DEFAULT_ADMIN_USER)
                .password(new SaltedPassword(DEFAULT_ADMIN_PASSWORD, null))
                .admin(true)
                .updatableProfile(true)
                .enabled(true);

        if (defaultAdmin != null) {
            log.error("No admin user where found, but the default user named '" + DEFAULT_ADMIN_USER + "'" +
                    " exists and is not admin!\n" +
                    "Updating the super user '" + DEFAULT_ADMIN_USER + "' with default state and password!");
            builder
                    .email(defaultAdmin.getEmail())
                    .lastLogin(defaultAdmin.getLastLoginTimeMillis(), defaultAdmin.getLastLoginClientIp())
                    .updatableProfile(true);

            updateUser(builder.build(), false);
        } else {
            createUser(builder.build());
        }
    }

    private void createDefaultAnonymousUser() {
        UserInfo anonymousUser = userGroupStoreService.findUser(UserInfo.ANONYMOUS);
        if (anonymousUser != null) {
            log.debug("Anonymous user " + anonymousUser + " already exists");
            return;
        }
        log.info("Creating the default anonymous user, since it does not exist!");
        UserInfoBuilder builder = new UserInfoBuilder(UserInfo.ANONYMOUS);
        builder.password(new SaltedPassword("", null)).email(null).enabled(true).updatableProfile(false);
        MutableUserInfo anonUser = builder.build();
        boolean createdAnonymousUser = createUser(anonUser);

        if (createdAnonymousUser) {
            MutableGroupInfo readersGroup = InfoFactoryHolder.get().createGroup("readers");
            readersGroup.setRealm(SecurityConstants.DEFAULT_REALM);
            readersGroup.setDescription("A group for read-only users");
            readersGroup.setNewUserDefault(true);
            createGroup(readersGroup);
            createDefaultSecurityEntities(anonUser, readersGroup, currentUsername());
        }
    }

    @Override
    public void createDefaultSecurityEntities(UserInfo anonUser, GroupInfo readersGroup, String currentUsername) {
        if (!UserInfo.ANONYMOUS.equals(anonUser.getUsername())) {
            throw new IllegalArgumentException(
                    "Default anything permissions should be created for the anonymous user only");
        }

        // create or update read permissions on "anything"
        AclInfo anyAnyAcl = getAcl(ANY_PERMISSION_TARGET_NAME);
        Set<AceInfo> anyAnyAces = new HashSet<>(2);
        anyAnyAces.add(InfoFactoryHolder.get().createAce(
                anonUser.getUsername(), false, READ.getMask()));
        anyAnyAces.add(InfoFactoryHolder.get().createAce(
                readersGroup.getGroupName(), true, READ.getMask()));
        if (anyAnyAcl == null) {
            MutablePermissionTargetInfo anyAnyTarget = InfoFactoryHolder.get().createPermissionTarget(
                    ANY_PERMISSION_TARGET_NAME, Lists.newArrayList((ANY_REPO)));
            anyAnyTarget.setIncludesPattern(ANY_PATH);
            anyAnyAcl = InfoFactoryHolder.get().createAcl(anyAnyTarget, anyAnyAces, currentUsername);
            aclStoreService.createAcl(anyAnyAcl);
        } else {
            MutableAclInfo acl = InfoFactoryHolder.get().createAcl(anyAnyAcl.getPermissionTarget());
            acl.setAces(anyAnyAces);
            acl.setUpdatedBy(currentUsername);
            aclStoreService.updateAcl(acl);
        }

        // create or update read and deploy permissions on all remote repos
        AclInfo anyRemoteAcl = getAcl(ANY_REMOTE_PERMISSION_TARGET_NAME);
        HashSet<AceInfo> anyRemoteAces = new HashSet<>(2);
        anyRemoteAces.add(InfoFactoryHolder.get().createAce(
                anonUser.getUsername(), false,
                READ.getMask() | DEPLOY.getMask()));
        if (anyRemoteAcl == null) {
            MutablePermissionTargetInfo anyRemoteTarget = InfoFactoryHolder.get().createPermissionTarget(
                    ANY_REMOTE_PERMISSION_TARGET_NAME, new ArrayList<String>() {{
                        add(ANY_REMOTE_REPO);
                    }});
            anyRemoteTarget.setIncludesPattern(ANY_PATH);
            anyRemoteAcl = InfoFactoryHolder.get().createAcl(anyRemoteTarget, anyRemoteAces, currentUsername);
            aclStoreService.createAcl(anyRemoteAcl);
        } else {
            MutableAclInfo acl = InfoFactoryHolder.get().createAcl(anyRemoteAcl.getPermissionTarget());
            acl.setAces(anyRemoteAces);
            acl.setUpdatedBy(currentUsername);
            aclStoreService.updateAcl(acl);
        }
    }

    @Override
    public void importSecurityData(String securityXml) {
        importSecurityData(new SecurityInfoReader().read(securityXml));
    }

    @Override
    public void importSecurityData(SecurityInfo securityInfo) {
        interceptors.onBeforeSecurityImport(securityInfo);
        clearSecurityListeners();

        try {
            accessService.importSecurityEntities(securityInfo);
        } finally {
            aclStoreService.invalidateAclCache();
        }

        boolean hasAnonymous = securityInfo.getUsers() != null && securityInfo.getUsers().stream().anyMatch(UserInfo::isAnonymous);
        if (!hasAnonymous) {
            createDefaultAnonymousUser();
        }
    }

    @Override
    public void addListener(SecurityListener listener) {
        securityListeners.add(listener);
    }

    @Override
    public void removeListener(SecurityListener listener) {
        securityListeners.remove(listener);
    }

    @Override
    public void authenticateAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(new SystemAuthenticationToken());
    }

    @Override
    public void doAsSystem(@Nonnull Runnable runnable) {
        Authentication originalAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            authenticateAsSystem();
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
        }
    }

    @Override
    public void nullifyContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public SaltedPassword generateSaltedPassword(String rawPassword) {
        return generateSaltedPassword(rawPassword, getDefaultSalt());
    }

    @Override
    public SaltedPassword generateSaltedPassword(@Nonnull String rawPassword, @Nullable String salt) {
        return new SaltedPassword(rawPassword, salt);
    }

    @Override
    public String getDefaultSalt() {
        return ConstantValues.defaultSaltValue.getString();
    }

    @Override
    public BasicStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        return ldapService.testLdapConnection(ldapSetting, username, password);
    }

    @Override
    public boolean isPasswordEncryptionEnabled() {
        CentralConfigDescriptor cc = centralConfig.getDescriptor();
        return cc.getSecurity().getPasswordSettings().isEncryptionEnabled();
    }

    @Override
    public boolean userPasswordMatches(String passwordToCheck) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && passwordToCheck.equals(authentication.getCredentials());
    }

    @Override
    public boolean canDeployToLocalRepository() {
        return !repositoryService.getDeployableRepoDescriptors().isEmpty();
    }

    private void clearSecurityListeners() {
        //Notify security listeners
        securityListeners.forEach(SecurityListener::onClearSecurity);
    }

    private void assertAdmin() {
        if (!isAdmin()) {
            throw new SecurityException(
                    "The attempted action is permitted to users with administrative privileges only.");
        }
    }

    /**
     * Validates that the edited given permission target is not different from the existing one. This method should be
     * called before an ACL is being modified by a non-sys-admin user
     *
     * @param newInfo Edited permission target
     * @throws AuthorizationException Thrown in case an unauthorized modification has occurred
     */
    private void validateUnmodifiedPermissionTarget(PermissionTargetInfo newInfo) throws AuthorizationException {
        if (newInfo == null) {
            return;
        }

        AclInfo oldAcl = getAcl(newInfo);
        if (oldAcl == null) {
            return;
        }

        PermissionTargetInfo oldInfo = oldAcl.getPermissionTarget();
        if (oldInfo == null) {
            return;
        }

        Sets.SetView<String> excludes = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getExcludes()),
                Sets.newHashSet(newInfo.getExcludes()));
        if (!excludes.isEmpty()) {
            alertModifiedField("excludes pattern");
        }

        if (!oldInfo.getExcludesPattern().equals(newInfo.getExcludesPattern())) {
            alertModifiedField("exclude pattern");
        }

        Sets.SetView<String> includes = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getIncludes()),
                Sets.newHashSet(newInfo.getIncludes()));
        if (!includes.isEmpty()) {
            alertModifiedField("include pattern");
        }

        if (!oldInfo.getIncludesPattern().equals(newInfo.getIncludesPattern())) {
            alertModifiedField("include pattern");
        }
        // make repo keys compatible with acl cached data
        List<String> compatibleRepoKeys = makeRemoteRepoKeysAclCompatible(newInfo.getRepoKeys());
        // getAndCheckAllUserAcls repo keys data , make sure that old repo data and new repo data is the same
        Sets.SetView<String> repoKeys = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getRepoKeys()),
                Sets.newHashSet(compatibleRepoKeys));
        if (!repoKeys.isEmpty()) {
            alertModifiedField("repositories");
        }
    }

    /**
     * Throws an AuthorizationException alerting an un-authorized change of configuration
     *
     * @param modifiedFieldName Name of modified field
     */
    private void alertModifiedField(String modifiedFieldName) {
        throw new AuthorizationException("User is not permitted to modify " + modifiedFieldName);
    }

    /**
     * Retrieves the Async advised instance of the service
     *
     * @return InternalSecurityService - Async advised instance
     */
    private InternalSecurityService getAdvisedMe() {
        return context.beanForType(InternalSecurityService.class);
    }

    @Override
    public List<String> convertCachedRepoKeysToRemote(List<String> repoKeys) {
        List<String> altered = Lists.newArrayList();
        for (String repoKey : repoKeys) {
            String repoKeyCacheOmitted;

            if (repoKey.contains(LocalCacheRepoDescriptor.PATH_SUFFIX)) {
                repoKeyCacheOmitted = repoKey.substring(0,
                        repoKey.lastIndexOf(LocalCacheRepoDescriptor.PATH_SUFFIX.charAt(0)));
            } else {
                altered.add(repoKey);
                continue;
            }
            if (repositoryService.remoteRepoDescriptorByKey(repoKeyCacheOmitted) != null) {
                altered.add(repoKeyCacheOmitted);
            } else {
                altered.add(repoKey); //Its Possible that someone named their local repo '*-cache'
            }
        }
        return altered;
    }

    /**
     * Converts remote repo keys contained in the list to have the '-cache' suffix as acls currently
     * only support this notation.
     *
     * @return repoKeys with all remote repository keys concatenated with '-cache' suffix
     */
    private List<String> makeRemoteRepoKeysAclCompatible(List<String> repoKeys) {
        List<String> altered = Lists.newArrayList();
        for (String repoKey : repoKeys) {
            if (repositoryService.remoteRepoDescriptorByKey(repoKey) != null) {
                altered.add(repoKey.concat(LocalCacheRepoDescriptor.PATH_SUFFIX));
            } else {
                altered.add(repoKey);
            }
        }
        return altered;
    }

    private String makeRemoteRepoKeyAclCompatible(String repoKey) {
        List<String> repoKeyAsList = new ArrayList<>();
        repoKeyAsList.add(repoKey);
        return (makeRemoteRepoKeysAclCompatible(repoKeyAsList).get(0));
    }

    private MutableAclInfo makeNewAclRemoteRepoKeysAclCompatible(MutableAclInfo acl) {
        //Make repository keys acl-compatible before update
        MutablePermissionTargetInfo mutablePermissionTargetInfo = InfoFactoryHolder.get().copyPermissionTarget
                (acl.getPermissionTarget());
        List<String> compatibleRepoKeys = makeRemoteRepoKeysAclCompatible(mutablePermissionTargetInfo.getRepoKeys());
        mutablePermissionTargetInfo.setRepoKeys(compatibleRepoKeys);
        acl.setPermissionTarget(mutablePermissionTargetInfo);

        return acl;
    }

    @Override
    public MutableAclInfo convertNewAclCachedRepoKeysToRemote(MutableAclInfo acl) {
        //Make repository keys acl-compatible before update
        MutablePermissionTargetInfo mutablePermissionTargetInfo = InfoFactoryHolder.get().copyPermissionTarget
                (acl.getPermissionTarget());
        List<String> compatibleRepoKeys = convertCachedRepoKeysToRemote(mutablePermissionTargetInfo.getRepoKeys());
        mutablePermissionTargetInfo.setRepoKeys(compatibleRepoKeys);
        acl.setPermissionTarget(mutablePermissionTargetInfo);

        return acl;
    }

    /**
     * Makes user password expired
     */
    @Override
    public void expireUserCredentials(String userName) {
        try {
            if (unknownUsersCache.getIfPresent(userName) != null) {
                log.debug("User {} is registered in unknown users cache, no password to expire ...");
                throw new UsernameNotFoundException("User " + userName + " does not exist");
            }
            UserInfo user = findUser(userName);
            if (!user.isCredentialsExpired()) {
                userGroupStoreService.expireUserPassword(userName);
                invalidateAuthCacheEntries(userName);
                dockerTokenManager.revokeToken(userName);
                auditLog.expireUserCredentials(userName);
            }
        } catch (StorageException e) {
            throw new PasswordExpireException(
                    "Expiring password for \"" + userName + "\" has failed, " + e.getMessage(), e);
        } catch (UsernameNotFoundException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordExpireException("Expiring password has failed, " + e.getMessage());
        }
    }

    /**
     * Makes user password expired
     */
    @Override
    public void unexpirePassword(String userName) {
        try {
            if (unknownUsersCache.getIfPresent(userName) != null) {
                log.debug("User {} is registered in unknown users cache, no password to expire ...");
                throw new UsernameNotFoundException("User " + userName + " does not exist");
            }
            findUser(userName); // todo: [mp] use plain user fetch (rather than heavy groups join)
            userGroupStoreService.revalidatePassword(userName);
            auditLog.unexpireUserPassword(userName);
        } catch (StorageException e) {
            throw new PasswordExpireException(
                    "Expiring password for \"" + userName + "\" has failed, " + e.getMessage(), e);
        } catch (UsernameNotFoundException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordExpireException("Expiring password has failed, " + e.getMessage());
        }
    }

    /**
     * Makes all users passwords expired
     */
    @Override
    public void expireCredentialsForAllUsers() {
        try {
            userGroupStoreService.expirePasswordForAllUsers();
            //Invalidate all auth caches
            invalidateAuthCacheEntriesForAllUsers();
            dockerTokenManager.revokeAllTokens();
            auditLog.expireAllUserCredentials();
        } catch (StorageException e) {
            log.debug("Expiring all users credentials have failed, cause: {}", e);
            throw new PasswordExpireException("Expiring all users credentials have failed, see logs for more details");
        }
    }

    /**
     * Makes all users passwords not expired
     */
    @Override
    public void unexpirePasswordForAllUsers() {
        try {
            userGroupStoreService.revalidatePasswordForAllUsers();
            auditLog.unexpireAllUserPasswords();
        } catch (StorageException e) {
            log.debug("Un-expiring all users credentials have failed, cause: {}", e);
            throw new PasswordExpireException(
                    "Un-expiring all users credentials have failed, see logs for more details");
        }
    }


    /**
     * Fetches users with password is about to expire
     *
     * @return list of users
     */
    @Override
    public Set<PasswordExpiryUser> getUsersWhichPasswordIsAboutToExpire() {
        return userGroupStoreService.getUsersWhichPasswordIsAboutToExpire(
                ConstantValues.passwordDaysToNotifyBeforeExpiry.getInt(), getPasswordExpirationDays());
    }

    /**
     * Marks user.credentialsExpired=True where password has expired
     *
     * @param daysToKeepPassword after what period password should be changed
     */
    @Override
    public void markUsersCredentialsExpired(int daysToKeepPassword) {
        List<String> expiredUsers = userGroupStoreService.markUsersCredentialsExpired(daysToKeepPassword);
        expiredUsers.forEach(user -> {
            invalidateAuthCacheEntries(user);
            dockerTokenManager.revokeToken(user);
        });
    }


    /**
     * @return number of days left till password will expire
     * or negative value if password already expired
     * or NULL if password expiration feature is disabled
     */
    @Override
    public Integer getUserPasswordDaysLeft(String userName) {
        Integer daysLeft = null;
        if (isPasswordExpirationPolicyEnabled()) {
            UserInfo user = userGroupStoreService.findUser(userName);
            if (user != null && !user.isAnonymous() && !user.isPasswordDisabled()) {
                Long userPasswordCreationTime = userGroupStoreService.getUserPasswordCreationTime(userName);
                if (userPasswordCreationTime != null) {
                    daysLeft = getDaysLeftUntilPasswordExpires(userPasswordCreationTime);
                } else {
                    log.debug("Password creation time for user {} returned no value", userName);
                }
            }
        }
        return daysLeft;
    }

    private Integer getDaysLeftUntilPasswordExpires(Long userPasswordCreationTime) {
        Integer daysLeft;
        DateTime created = new DateTime(userPasswordCreationTime.longValue());
        int expiresIn = getPasswordExpirationDays();
        DateTime now = DateTime.now();
        daysLeft = created.plusDays(expiresIn).minusDays(now.getDayOfYear()).getDayOfYear();
        if ((daysLeft == 365 || daysLeft == 366) && created.plusDays(expiresIn).dayOfYear().get() != daysLeft) {
            daysLeft = 0;
        }
        return daysLeft;
    }
}

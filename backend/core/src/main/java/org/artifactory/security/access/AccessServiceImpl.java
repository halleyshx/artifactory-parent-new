package org.artifactory.security.access;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.access.*;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.accesstoken.AccessClientSettings;
import org.artifactory.event.CacheType;
import org.artifactory.event.InvalidateCacheEvent;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.security.*;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.security.service.access.AclMapper;
import org.artifactory.storage.db.security.service.access.GroupMapper;
import org.artifactory.storage.db.security.service.access.UserMapper;
import org.artifactory.storage.fs.service.ConfigsService;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.access.client.*;
import org.jfrog.access.client.http.RestRequest;
import org.jfrog.access.client.http.RestResponse;
import org.jfrog.access.client.token.TokenRequest;
import org.jfrog.access.client.token.TokenResponse;
import org.jfrog.access.client.token.TokenVerifyResult;
import org.jfrog.access.client.token.TokensInfoResponse;
import org.jfrog.access.common.AccessAuthz;
import org.jfrog.access.common.ServiceId;
import org.jfrog.access.common.SubjectFQN;
import org.jfrog.access.rest.imports.ImportEntitiesRequest;
import org.jfrog.access.rest.system.ConfigurationModel;
import org.jfrog.access.token.JwtAccessToken;
import org.jfrog.common.JsonUtils;
import org.jfrog.common.YamlUtils;
import org.jfrog.security.util.ULID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.concat;
import static java.lang.Thread.sleep;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.artifactory.common.ArtifactoryHome.ACCESS_SERVICE_ID;
import static org.artifactory.common.ArtifactoryHome.CLUSTER_ID;
import static org.artifactory.security.access.AccessServiceConstants.ARTIFACTORY_SERVICE_ANY_ID_PATTERN;
import static org.artifactory.security.access.AccessServiceConstants.ARTIFACTORY_SERVICE_TYPE;
import static org.artifactory.security.access.ArtifactoryAdminScopeToken.SCOPE_ARTIFACTORY_ADMIN_PATTERN;
import static org.artifactory.security.access.ArtifactoryRepoPathScopeToken.*;
import static org.artifactory.security.access.MemberOfGroupsScopeToken.SCOPE_MEMBER_OF_GROUPS_PATTERN;
import static org.jfrog.access.token.JwtAccessToken.SCOPE_DELIMITER;
import static org.jfrog.config.wrappers.ConfigurationManagerAdapter.normalizedFilesystemPath;
import static org.jfrog.security.file.SecurityFolderHelper.PERMISSIONS_MODE_644;
import static org.jfrog.security.file.SecurityFolderHelper.setPermissionsOnSecurityFile;

/**
 * @author Yinon Avraham
 */
@Service
@Lazy
@Reloadable(beanClass = AccessService.class, initAfter = InternalCentralConfigService.class,
        listenOn = {CentralConfigKey.securityAccessClientSettings, CentralConfigKey.securityPasswordSettings})
public class AccessServiceImpl implements AccessService, ContextReadinessListener {

    private static final Logger log = LoggerFactory.getLogger(AccessServiceImpl.class);
    private static final int MIN_INSTANCE_ID_LENGTH = 20;

    //Accepted scopes:
    private static final String SCOPE_API = "api:*";
    private static final Pattern SCOPE_API_PATTERN = Pattern.compile(Pattern.quote(SCOPE_API));

    private final List<Pattern> acceptedScopePatternsByNonAdmin = Lists
            .newArrayList(SCOPE_API_PATTERN, SCOPE_MEMBER_OF_GROUPS_PATTERN, SCOPE_ARTIFACTORY_REPO_PATH_PATTERN);
    private final List<Pattern> acceptedScopePatternsByAdmin = Lists.newArrayList(SCOPE_ARTIFACTORY_ADMIN_PATTERN);
    private final List<Pattern> acceptedScopePatterns = Lists.newArrayList(
            concat(acceptedScopePatternsByNonAdmin, acceptedScopePatternsByAdmin));

    public static final String ACCESS_BOOTSTRAP_JSON = "access.bootstrap.json";
    private static final String ARTIFACTORY_SERVICE_ID = "artifactory.service_id";

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private UserGroupStoreService userGroupStore;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private InternalCentralConfigService centralConfigService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ConfigsService configsService;

    private final ContextStateDependantActionRunner contextStateDependantActionRunner = new ContextStateDependantActionRunner();
    private ArtifactoryHome artifactoryHome;
    private ServiceId serviceId;
    private AccessClient accessClient;
    private ArtifactoryAccessClientConfigStore configStore;
    private final Object accessClientLock = new Object();
    private AccessAuthCredentials accessAdminCredentials;

    SecurityService securityService() {
        return securityService;
    }

    InternalCentralConfigService centralConfigService() {
        return centralConfigService;
    }

    ArtifactoryHome artifactoryHome() {
        return artifactoryHome;
    }

    @Override
    public void registerAcceptedScopePattern(@Nonnull Pattern pattern) {
        synchronized (acceptedScopePatterns) {
            log.debug("Registering accepted scope pattern: {}", requireNonNull(pattern, "pattern is required"));
            if (!acceptedScopePatterns.stream().anyMatch(p -> p.pattern().equals(pattern.pattern()))) {
                acceptedScopePatterns.add(pattern);
            } else {
                log.debug("Pattern already exists in the accepted scope patterns: '{}'", pattern);
            }
        }
    }

    @Nonnull
    @Override
    public List<TokenInfo> getTokenInfos() {
        try {
            TokensInfoResponse tokensInfoResponse = accessClient.token().getTokensInfo();
            List<TokenInfo> result = tokensInfoResponse.getTokens().stream()
                    .map(this::toTokenInfo)
                    .filter(this::isNonInternalToken)
                    .collect(Collectors.toList());
            return result;
        } catch (AccessClientException e) {
            throw new RuntimeException("Failed to get tokens information.", e);
        }
    }

    @Override
    public AccessClient getAccessClient() {
        synchronized (accessClientLock) {
            return accessClient;
        }
    }

    @Override
    public void encryptOrDecrypt(boolean encrypt) {
        configStore.encryptOrDecryptAccessCreds(encrypt);
    }

    private TokenInfo toTokenInfo(org.jfrog.access.client.token.TokenInfo clientTokenInfo) {
        return new TokenInfoImpl(clientTokenInfo.getTokenId(),
                clientTokenInfo.getIssuer(),
                clientTokenInfo.getSubject(),
                clientTokenInfo.getExpiry(),
                clientTokenInfo.getIssuedAt(),
                clientTokenInfo.isRefreshable());
    }

    private boolean isNonInternalToken(TokenInfo tokenInfo) {
        try {
            SubjectFQN subject = SubjectFQN.fromFullyQualifiedName(tokenInfo.getSubject());
            if (subject.getServiceId().equals(getArtifactoryServiceId())) {
                if (!UserTokenSpec.isUserToken(tokenInfo)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void init() {
        artifactoryHome = ArtifactoryHome.get();
        initServiceId();
        initIfNeeded();
        updateAccessConfiguration(centralConfigService.getDescriptor().getSecurity());
    }

    private void initIfNeeded() {
        if (accessClient == null) {
            initAccessService(true);
        }
    }

    @Override
    public void initAccessService(boolean doBootstrap) {
        synchronized (accessClientLock) {
            AccessClient oldAccessClient = this.accessClient;
            try {
                configStore = new ArtifactoryAccessClientConfigStore(this, serviceId);
                this.accessClient = configStore.newClientBuilder().create();
                long secondsToWait = ConstantValues.accessClientWaitForServer.getLong(artifactoryHome);
                waitForAccessServer(TimeUnit.SECONDS.toMillis(secondsToWait));
                if (doBootstrap) {
                    AccessClientBootstrap bootstrap = new AccessClientBootstrap(configStore, this.accessClient);
                    this.accessClient = bootstrap.getAccessClient(); //access client can be updated with the admin token
                    Pair<String, String> accessAdmin = bootstrap.getAdminCredentials();
                    this.accessAdminCredentials = new AccessAuthCredentials(accessAdmin.getLeft(),
                            accessAdmin.getRight());
                }
                invalidateAdminCredentials();
                log.info("Initialized access service successfully with client id {}, closing old client id {}",
                        this.accessClient.hashCode(), oldAccessClient != null ? oldAccessClient.hashCode() : "[null]");
            } finally {
                IOUtils.closeQuietly(oldAccessClient);
            }
        }
    }

    private void waitForAccessServer(long timeoutMillis) {
        log.info("Waiting for access server...");
        long startTime = System.currentTimeMillis();
        long now = startTime;
        boolean success = false;
        AccessClient accessClientNoAuth = this.accessClient.useAuth(null);
        while (now - startTime < timeoutMillis) {
            try {
                log.debug("Pinging access server...");
                success = accessClientNoAuth.ping();
                if (success) {
                    log.info("Got response from Access server after {} ms, continuing.",
                            System.currentTimeMillis() - startTime);
                    break;
                }
            } catch (AccessClientException e) {
                log.debug("Could not ping access server: {}", e.toString());
                //ignore - assuming 404 or 503 and such. In the end, if ping is not successful then it will anyway fail.
            }
            log.debug("Pinging access server did not succeed, waiting for 500ms before retrying...");
            pause();
            now = System.currentTimeMillis();
        }
        if (!success) {
            throw new IllegalStateException("Waiting for access server to respond timed-out after " +
                    (System.currentTimeMillis() - startTime) + " milliseconds.");
        }
    }

    /**
     * We try two ways:
     * 1. Look for the serviceId file at <ETC>/access/keys/ACCESS_SERVICE_ID
     * It could be either from an old Artifactory version, or due to system import.
     * In case its Pro or HA Master we sync the file to DB. On both master and nodes we revoke admin token.
     * User needs to restart other nodes for the change to take affect.
     * In any case (HA or not), if the file exists we rename the file at the local storage to <name>.<timestamp>.bak
     *
     * 2. Look for the file at DB config table, and set it's value to the object in-memory.
     * In case it doesn't exist in file storage or in DB we create it and store it in DB.
     * If storing in DB fails --> sleep, try to read from DB (other node could have initialized it already)
     *
     * 3. If all of the above fails --> Goodbye.
     */
    private void initServiceId() {
        String oldServiceId = configsService.getConfig(ARTIFACTORY_SERVICE_ID);
        // Try to import from filesystem if serviceId file exist
        tryToImportServiceId();
        // Create/load serviceId
        String serviceId = configsService.getConfig(ARTIFACTORY_SERVICE_ID);
        if (StringUtils.isNotBlank(serviceId)) {
            this.serviceId = ServiceId.fromFormattedName(serviceId);
            log.debug("Successfully loaded serviceId from DB");
            // In case serviceId have changed, revoke current admin token so we won't get wrong issuerId at propagation
            if (!StringUtils.equals(oldServiceId, serviceId)) {
                initIfNeeded();
                configStore.revokeAdminToken();
            }
        } else {
            // Case of first Artifactory startup
            String instanceId = generateServiceInstanceID();
            this.serviceId = new ServiceId(ARTIFACTORY_SERVICE_TYPE, instanceId);
            try {
                configsService.addConfig(ARTIFACTORY_SERVICE_ID, this.serviceId.toString(), System.currentTimeMillis());
                log.debug("Successfully generated serviceId and uploaded to DB");
            } catch (StorageException e) {
                // Failed over insert, other node insert serviceId already, get what is inside DB.
                log.debug("Couldn't upload newly generated serviceId to DB, retrying reading from DB for existing one");
                pause();
                serviceId = configsService.getConfig(ARTIFACTORY_SERVICE_ID);
                if (StringUtils.isNotBlank(serviceId)) {
                    this.serviceId = ServiceId.fromFormattedName(serviceId);
                    log.debug("Successfully loaded serviceId from DB");
                } else {
                    //This means a true sql error, throw it.
                    throw new RuntimeException("Failed to create/load serviceId", e);
                }
            }
        }
    }

    private void tryToImportServiceId() {
        // Trying to load service id from file system (could be either system import or old Artifactory version)
        File serviceIdFile = getAccessServiceIdFile();
        // Support legacy cluster file and corrupted states where cluster.id file exist but service_id file not
        File clusterIdFile = getClusterIdFile();
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        // In case HA&&Primary or Not-HA we read the file and update DB
        if(!haCommonAddon.isHaEnabled() || haCommonAddon.isPrimary()) {
            try {
                File candidate = chooseBestCandidate(serviceIdFile,clusterIdFile);
                // If candidate file exists, import it and delete it. Otherwise, do nothing.
                if (candidate.exists()) {
                    log.debug("Instance is HA-Primary or Pro, handling serviceId local file");
                    String formattedServiceId = Files.readAllLines(candidate.toPath()).get(0);
                    String dbServiceId = configsService.getConfig(ARTIFACTORY_SERVICE_ID);
                    // Update db
                    if (StringUtils.isNotBlank(dbServiceId)) {
                        // Case there is an old serviceId in DB
                        overwriteServiceId(formattedServiceId, dbServiceId);
                    } else {
                        // Case there is no serviceId in DB
                        configsService
                                .addConfig(ARTIFACTORY_SERVICE_ID, formattedServiceId, System.currentTimeMillis());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize the service ID.", e);
            }
        } else {
            log.debug("Instance is HA non-primary, no need to handle serviceId local file");
        }
        // Rename the file to avoid future imports (even on non-primary nodes)
        renameOldServiceIdName(serviceIdFile);
        renameOldServiceIdName(clusterIdFile);
    }

    private File chooseBestCandidate(File serviceIdFile, File clusterIdFile) {
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        if(!serviceIdFile.exists() && haCommonAddon.isHaConfigured()){
            return clusterIdFile;
        }
        return serviceIdFile;
    }

    private void overwriteServiceId(String formattedServiceId, String dbServiceId) {
        configsService
                .updateConfig(ARTIFACTORY_SERVICE_ID, formattedServiceId, System.currentTimeMillis());
        log.warn("ServiceId overwrite: detected {} file. In case of HA: if you want to set a new " +
                 "ServiceId you need to restart each node. Old value: {}, new value: {}",
                ACCESS_SERVICE_ID, dbServiceId, formattedServiceId);
    }

    private File getAccessServiceIdFile() {
        return new File(artifactoryHome.getAccessClientDir(), normalizedFilesystemPath("keys", ACCESS_SERVICE_ID));
    }

    private File getClusterIdFile() {
        return new File(artifactoryHome.getEtcDir(), CLUSTER_ID);
    }

    private void renameOldServiceIdName(File file) {
        // If file doesn't exist no need to move it.
        if (!file.exists()) {
            return;
        }
        long date = System.currentTimeMillis();
        File targetFile = new File(file.getAbsolutePath() + "." + date + ".bak");
        if (!file.renameTo(targetFile)) {
            log.warn("Failed to delete service id file from file system: {}", file.getAbsoluteFile());
        }
    }

    private ServiceId createServiceId(String serviceId) {
        return ServiceId.fromFormattedName(serviceId);
    }

    private String generateServiceInstanceID() {
        String id = ULID.random().toLowerCase();
        return normalizeInstanceId(id);
    }

    static String normalizeInstanceId(String id) {
        Matcher matcher = ServiceId.ELEMENT_PATTERN.matcher(id);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            //Fill the gap (if any)
            for (int i = builder.length(); i < matcher.start(); i++) {
                builder.append("_");
            }
            builder.append(matcher.group());
        }
        String normalizedId = builder.toString();
        if (normalizedId.length() < MIN_INSTANCE_ID_LENGTH) {
            throw new IllegalArgumentException("Instance ID is too short. (normalized='" + normalizedId +
                    "', original='" + id + "')");
        }
        return normalizedId;
    }

    @Nonnull
    @Override
    public TokenResponse createTokenWithAccessAdminCredentials(@Nonnull String serviceIdAsString) {
        try {
            ServiceId serviceId = createServiceId(serviceIdAsString);
            List<String> scopes = Collections.singletonList(AccessAuthz.ADMIN);
            TokenRequest tokenRequest = new TokenRequest(scopes, false, serviceId.toString(),
                    AccessClientBootstrap.SERVICE_ADMIN_TOKEN_EXPIRY, null, emptyList());
            assertAdminUserCanCreateToken(tokenRequest);
            tokenRequest = replacePathTokenScopeWithChecksum(tokenRequest);
            return accessClient.token().useAuth(accessAdminCredentials).create(tokenRequest);
        } catch (AccessClientException e) {
            log.error("Failed to create token for subject '{}': {}", serviceId.toString(), e.getMessage());
            log.debug("Failed to create token for subject '{}'", serviceId.toString(), e);
            throw new RuntimeException("Failed to create token for subject '" + serviceId.toString() + "'.", e);
        }

    }

    @Override
    @Nonnull
    public CreatedTokenInfo createToken(@Nonnull TokenSpec tokenSpec) {
        String subject = null;
        try {
            subject = tokenSpec.createSubject(serviceId).toString();
            List<String> scope = getEffectiveScope(tokenSpec);
            List<String> audience = getEffectiveAudience(tokenSpec);
            TokenRequest tokenRequest = new TokenRequest(scope, tokenSpec.isRefreshable(), subject,
                    tokenSpec.getExpiresIn(), null, audience);
            assertLoggedInCanCreateToken(tokenRequest);
            tokenRequest = replacePathTokenScopeWithChecksum(tokenRequest);
            TokenResponse tokenResponse = accessClient.token().create(tokenRequest);
            return toCreatedTokenInfo(tokenResponse, scope);
        } catch (AccessClientException e) {
            log.error("Failed to create token for subject '{}': {}", subject, e.getMessage());
            log.debug("Failed to create token for subject '{}'", subject, e);
            throw new RuntimeException("Failed to create token for subject '" + subject + "'.", e);
        }
    }

    @Nonnull
    @Override
    public CreatedTokenInfo createNoPermissionToken(@Nonnull TokenSpec tokenSpec, @Nullable String extraData) {
        String subject = null;
        try {
            subject = tokenSpec.createSubject(serviceId).toString();
            List<String> scope = Lists.newArrayList("dummy");
            List<String> audience = getEffectiveAudience(tokenSpec);
            TokenRequest tokenRequest = new TokenRequest(scope, tokenSpec.isRefreshable(), subject,
                    tokenSpec.getExpiresIn(), extraData, audience);
            tokenRequest = replacePathTokenScopeWithChecksum(tokenRequest);
            TokenResponse tokenResponse = accessClient.token().create(tokenRequest);
            return toCreatedTokenInfo(tokenResponse, scope);
        } catch (AccessClientException e) {
            log.error("Failed to create token for subject '{}': {}", subject, e.getMessage());
            log.debug("Failed to create token for subject '{}'", subject, e);
            throw new RuntimeException("Failed to create token for subject '" + subject + "'.", e);
        }
    }

    /**
     * Replace all ArtifactoryRepoPathScopeToken tokens with a checksum repoPath.
     */
    private TokenRequest replacePathTokenScopeWithChecksum(TokenRequest tokenRequest) {
        List<String> newScope = Lists.newArrayList();
        List<String> scope = tokenRequest.getScope();
        for (String scopeToken : scope) {
            if (ArtifactoryRepoPathScopeToken.accepts(scopeToken)) {
                ArtifactoryRepoPathScopeToken parse = ArtifactoryRepoPathScopeToken.parse(scopeToken);
                if (!parse.isChecksumPath()) {
                    newScope.add(
                            PATH_CHECKSUM_PREFIX + ":" + parse.getRepoPathChecksum() + ":" + parse.getPermissions());
                } else { // already checksum
                    newScope.add(scopeToken);
                }
            } else {
                newScope.add(scopeToken);
            }
        }
        return new TokenRequest(newScope, tokenRequest.isRefreshable(), tokenRequest.getSubject(),
                tokenRequest.getExpiresIn(),
                tokenRequest.getExtension(), tokenRequest.getAudience());
    }

    private void assertLoggedInCanCreateToken(TokenRequest tokenRequest) {
        String currentUsername = authorizationService.currentUsername();
        if (authorizationService.isAdmin() || SecurityService.USER_SYSTEM.equals(currentUsername)) {
            assertAdminUserCanCreateToken(tokenRequest);
        } else {
            assertNonAdminUserCanCreateToken(tokenRequest, currentUsername);
        }
        assertAllAudienceAreArtifactoryInstances(tokenRequest);
    }

    private void assertAllAudienceAreArtifactoryInstances(TokenRequest tokenRequest) {
        Optional<String> illegalAudience = tokenRequest.getAudience().stream()
                .filter(aud -> !ARTIFACTORY_SERVICE_ANY_ID_PATTERN.matcher(aud).matches())
                .findFirst();
        if (illegalAudience.isPresent()) {
            throw new AuthorizationException("Illegal audience: " + illegalAudience.get() + ", audience can contain " +
                    "only service IDs of Artifactory servers.");
        }
    }

    private void assertNonAdminUserCanCreateToken(TokenRequest tokenRequest, String currentUsername) {
        //non-admin users can only create tokens for themselves under this artifactory service ID
        if (!UserTokenSpec.isUserTokenSubject(tokenRequest.getSubject())) {
            throw new AuthorizationException("User " + currentUsername + " can only create user token for himself " +
                    "(requested: " + tokenRequest.getSubject() + ")");
        }
        String subjectUsername = UserTokenSpec.extractUsername(tokenRequest.getSubject());
        if (!currentUsername.equals(subjectUsername)) {
            throw new AuthorizationException("User " + currentUsername + " can only create user token for himself " +
                    "(requested: " + subjectUsername + ")");
        }
        ServiceId subjectServiceId = SubjectFQN.fromFullyQualifiedName(tokenRequest.getSubject()).getServiceId();
        if (!serviceId.equals(subjectServiceId)) {
            throw new AuthorizationException("User " + currentUsername + " can only create user token for himself " +
                    "under this Artifactory service ID (requested: " + tokenRequest.getSubject() + ")");
        }
        assertValidScopeForNonAdmin(tokenRequest.getScope());
        //non-admin users can have limited expires in
        assertValidExpiresInForNonAdmin(tokenRequest, currentUsername);
    }

    private void assertAdminUserCanCreateToken(TokenRequest tokenRequest) {
        tokenRequest.getScope().forEach(scopeToken -> {
            if (ArtifactoryAdminScopeToken.accepts(scopeToken)) {
                ServiceId serviceId = ArtifactoryAdminScopeToken.parse(scopeToken).getServiceId();
                if (!serviceId.equals(getArtifactoryServiceId())) {
                    throw new AuthorizationException("Admin can create token with admin privileges only on this " +
                            "Artifactory instance: " + getArtifactoryServiceId() +
                            " (requested: " + serviceId + ")");
                }
            }
            if (ArtifactoryRepoPathScopeToken.accepts(scopeToken)) {
                assertPathInTokenExists(ArtifactoryRepoPathScopeToken.getPath(scopeToken));
            }
        });
    }

    private void assertValidScopeForNonAdmin(List<String> scopes) {
        // check if any of the given scopes isn't supported by non admin user
        Optional<String> unsupportedScopeToken = scopes.stream()
                .filter(this::scopeUnsupportedForNonAdmin)
                .findFirst();
        if (unsupportedScopeToken.isPresent()) {
            throw new AuthorizationException("Logged in user cannot request token with scope: " +
                    unsupportedScopeToken.get());
        }
        assertValidGroupsForNonAdmin(scopes);
        assertValidRepoPathsForNonAdmin(scopes);
    }

    private boolean scopeUnsupportedForNonAdmin(String scopeToken) {
        return acceptedScopePatternsByNonAdmin.stream()
                .noneMatch(pattern -> pattern.matcher(scopeToken).matches());
    }

    /**
     * Assert non admin user has permissions on given path, and that those paths exists
     */
    private void assertValidRepoPathsForNonAdmin(List<String> scope) {
        List<String> repoPathsFromScope = collectRepoPathsFromScope(scope);
        repoPathsFromScope.forEach(this::assertPathInTokenExists);
        boolean hasPermissionsOnAllRepos = scope.stream()
                .filter(ArtifactoryRepoPathScopeToken::accepts)
                .allMatch(this::hasPermissionsOnRepoPath);
        if (!hasPermissionsOnAllRepos) {
            throw new AuthorizationException("Logged in user cannot create repo path token with scope: " +
                    "no permissions on paths");
        }
    }

    private void assertPathInTokenExists(String repoPathName) {
        RepoPath repoPath = RepoPathFactory.create(repoPathName);
        ItemInfo itemInfo;
        try {
            itemInfo = repositoryService.getItemInfo(repoPath);
        } catch (ItemNotFoundRuntimeException nfe) {
            throw new NotFoundException(
                    "Cannot create a token with provided scope - path doesn't exist : " + repoPath.toString());
        }
        if (itemInfo.isFolder()) {
            throw new NotFoundException(
                    "Cannot create a token with provided scope - path is a directory : " + repoPath.toString());
        }
    }

    private boolean hasPermissionsOnRepoPath(String scopeToken) {
        RepoPath repoPath = RepoPathFactory.create(ArtifactoryRepoPathScopeToken.getPath(scopeToken));
        return READ_PERMISSION.equals(ArtifactoryRepoPathScopeToken.parse(scopeToken).getPermissions()) &&
                authorizationService.canRead(repoPath);
    }

    /**
     * for non-admin users - Check user is a member of requested groups
     */
    private void assertValidGroupsForNonAdmin(List<String> scope) {
        Set<String> requestedGroupNames = collectGroupNamesFromScope(scope);
        // In case the token scope contains member-of-groups:* there is no need to verify groups
        if (requestedGroupNames.size() == 1 && requestedGroupNames.contains("*")) {
            return;
        }
        UserInfo userInfo = userGroupService.currentUser();
        Set<UserGroupInfo> userGroups = userInfo.getGroups();
        Set<String> acceptedGroupNames = Optional.ofNullable(userGroups)
                .map(groups -> groups.stream()
                        .map(UserGroupInfo::getGroupName)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        requestedGroupNames.removeAll(acceptedGroupNames);
        if (!requestedGroupNames.isEmpty()) {
            throw new AuthorizationException("Logged in user is not a member of the following groups: " +
                    String.join(",", requestedGroupNames));
        }
    }

    void assertValidExpiresInForNonAdmin(TokenRequest tokenRequest, String currentUsername) {
        SecurityDescriptor securityDescriptor = centralConfigService.getDescriptor().getSecurity();
        AccessClientSettings accessClientSettings = securityDescriptor.getAccessClientSettings();
        long maxExpiresInMinutes = Optional.ofNullable(accessClientSettings)
                .map(settings -> Optional.ofNullable(settings.getUserTokenMaxExpiresInMinutes()))
                .orElse(Optional.empty())
                .orElse(AccessClientSettings.USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT);
        if (maxExpiresInMinutes != AccessClientSettings.USER_TOKEN_MAX_EXPIRES_IN_MINUTES_UNLIMITED) {
            long maxExpiresInSecs = maxExpiresInMinutes * 60;
            long expiresIn = Optional.ofNullable(tokenRequest.getExpiresIn()).orElse(Long.MAX_VALUE);
            if (expiresIn > maxExpiresInSecs) {
                throw new AuthorizationException("User " + currentUsername + " can only create user token with max " +
                        "expires in " + maxExpiresInSecs + " (requested: " + tokenRequest.getExpiresIn() + ")");
            }
        }
    }

    @Nonnull
    @Override
    public CreatedTokenInfo refreshToken(@Nonnull TokenSpec tokenSpec, @Nonnull String tokenValue,
            @Nonnull String refreshToken) {
        JwtAccessToken accessToken = parseToken(tokenValue);
        assertTokenCreatedByThisService(accessToken);
        try {
            List<String> scope = isNotEmpty(tokenSpec.getScope()) ? tokenSpec.getScope() : accessToken.getScope();
            //false if specified, true otherwise (a refreshable token keeps to be refreshable, unless specified otherwise)
            boolean refreshable = Boolean.FALSE.equals(tokenSpec.getRefreshable());
            List<String> audience =
                    isNotEmpty(tokenSpec.getAudience()) ? tokenSpec.getAudience() : accessToken.getAudience();
            TokenRequest tokenRequest = new TokenRequest(scope, refreshable, accessToken.getSubject(),
                    tokenSpec.getExpiresIn(), null, audience);
            TokenResponse tokenResponse = accessClient.token().refresh(refreshToken, tokenRequest);
            return toCreatedTokenInfo(tokenResponse, scope);
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() == 404) {
                throw new TokenNotFoundException("Invalid access token or refresh token", e);
            } else if (e.getStatusCode() == 403) {
                throw new AuthorizationException("Refresh token operation rejected", e);
            }
            throw new RuntimeException("Access server refused to refresh the token", e);
        } catch (AccessClientException e) {
            log.error("Failed to refresh token for subject '{}': {}", accessToken.getSubject(), e.getMessage());
            log.debug("Failed to refresh token with id '{}' for subject '{}'",
                    accessToken.getTokenId(), accessToken.getSubject(), e);
            throw new RuntimeException("Failed to refresh token for subject '" + accessToken.getSubject() + "'.", e);
        }
    }

    /**
     * Asserts that the given token was created by this service (artifactory instance/cluster). This method accepts both
     * access tokens and refresh tokens, to support revoke by both. In case the token is not a valid access token (e.g.
     * a refresh token) the token is not checked.
     *
     * @param tokenValue the token to check, can be either an access token or a refresh token
     */
    private void assertTokenCreatedByThisService(@Nonnull String tokenValue) {
        try {
            JwtAccessToken accessToken = parseToken(tokenValue);
            assertTokenCreatedByThisService(accessToken);
        } catch (IllegalArgumentException e) {
            log.debug("Could not parse token value, it might be a refresh token, ignoring.", e);
        }
    }

    /**
     * Asserts that the given token was created by this service (artifactory instance/cluster).
     * This is currently important for refreshing and revoking tokens because it can only be done by the same service.
     *
     * @param accessToken the access token to check
     */
    private void assertTokenCreatedByThisService(@Nonnull JwtAccessToken accessToken) {
        String issuer = accessToken.getIssuer();
        ServiceId issuerServiceId = ServiceId.fromFormattedName(issuer);
        if (!issuerServiceId.equals(serviceId)) {
            throw new TokenIssuedByOtherServiceException("Provided access token with ID '" + accessToken.getTokenId() +
                    "' was issued by a different service with ID '" + issuerServiceId + "' (current service ID: '" +
                    serviceId + "')", serviceId, issuerServiceId);
        }
    }

    private CreatedTokenInfo toCreatedTokenInfo(TokenResponse tokenResponse, List<String> scopeTokens) {
        String scope = String.join(SCOPE_DELIMITER, scopeTokens);
        OptionalLong expiresInOptional = tokenResponse.getExpiresIn();
        Long expiresIn = expiresInOptional.isPresent() ? expiresInOptional.getAsLong() : null;
        return new CreatedTokenInfoImpl(tokenResponse.getTokenValue(), tokenResponse.getTokenType(),
                tokenResponse.getRefreshToken().orElse(null), scope, expiresIn);
    }

    private List<String> getEffectiveScope(TokenSpec tokenSpec) {
        List<String> effectiveScope = Lists.newArrayList(tokenSpec.getScope());
        addUserGroupsToScopeIfNeeded(effectiveScope, tokenSpec);
        addApiToScopeIfNeeded(effectiveScope);
        assertAcceptedScope(effectiveScope);
        return effectiveScope;
    }

    private void assertAcceptedScope(List<String> scope) {
        scope.stream()
                .filter(scopeToken ->
                        !acceptedScopePatterns.stream().anyMatch(pattern -> pattern.matcher(scopeToken).matches()))
                .findFirst()
                .ifPresent(scopeToken -> {
                    throw new IllegalArgumentException("Unaccepted scope: '" + scopeToken + "'");
                });
        if (scope.isEmpty() || scope.equals(singletonList(SCOPE_API))) {
            throw new IllegalArgumentException("Insufficient scope: '" + String.join(SCOPE_DELIMITER, scope) + "'");
        }
    }

    private void addApiToScopeIfNeeded(List<String> scope) {
        if (!scope.contains(SCOPE_API)) {
            scope.add(SCOPE_API);
        }
    }

    private void addUserGroupsToScopeIfNeeded(List<String> scope, TokenSpec tokenSpec) {
        if (tokenSpec instanceof UserTokenSpec) {
            UserInfo user = userGroupStore.findUser(((UserTokenSpec) tokenSpec).getUsername());
            if (user != null) {
                //Add user's assigned groups by default
                if (scope.isEmpty() || scope.equals(singletonList(SCOPE_API))) {
                    Set<UserGroupInfo> groups = user.getGroups();
                    if (groups != null && !groups.isEmpty()) {
                        List<String> groupNames = groups.stream()
                                .map(UserGroupInfo::getGroupName)
                                .collect(Collectors.toList());
                        String groupsConcat = String.join(",", groupNames);
                        scope.add("member-of-groups:" + groupsConcat);
                    }
                }
            }
        }
    }

    private Set<String> collectGroupNamesFromScope(List<String> scope) {
        return scope.stream()
                .filter(MemberOfGroupsScopeToken::accepts)
                .flatMap(scopeToken -> MemberOfGroupsScopeToken.parse(scopeToken).getGroupNames().stream())
                .collect(Collectors.toSet());
    }

    /**
     * collect all ArtifactoryRepoPathScopeToken repoPaths from scope
     */
    private List<String> collectRepoPathsFromScope(List<String> scope) {
        return scope.stream()
                .filter(ArtifactoryRepoPathScopeToken::accepts)
                .map(ArtifactoryRepoPathScopeToken::getPath)
                .collect(Collectors.toList());
    }

    private List<String> getEffectiveAudience(TokenSpec tokenSpec) {
        List<String> effectiveAudience = Lists.newArrayList(tokenSpec.getAudience());
        // If audience was not specified - use this service by default, otherwise use the specified audience,
        // even if it does not contain this service.
        if (effectiveAudience.isEmpty()) {
            String thisServiceIdName = serviceId.getFormattedName();
            effectiveAudience.add(thisServiceIdName);
        } else {
            // Replace "any-type" with the artifactory type (creating tokens through artifactory allows targeting only artifactory.)
            effectiveAudience = effectiveAudience.stream()
                    .map(aud -> {
                        if ("*".equals(aud) || "*@*".equals(aud)) {
                            return ARTIFACTORY_SERVICE_TYPE + "@*";
                        }
                        return aud;
                    }).collect(Collectors.toList());
        }
        return effectiveAudience;
    }

    @Nullable
    @Override
    public String extractSubjectUsername(@Nonnull JwtAccessToken accessToken) {
        try {
            return UserTokenSpec.extractUsername(accessToken.getSubject());
        } catch (Exception e) {
            log.debug("Failed to extract subject username from access token: {}", accessToken, e);
            return null;
        }
    }

    @Override
    @Nonnull
    public Collection<String> extractAppliedGroupNames(@Nonnull JwtAccessToken accessToken) {
        return collectGroupNamesFromScope(accessToken.getScope());
    }

    @Override
    public void revokeToken(@Nonnull String tokenValue) {
        assertTokenCreatedByThisService(tokenValue);
        try {
            accessClient.token().revoke(tokenValue);
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() == 404) {
                throw new TokenNotFoundException("Invalid access token or refresh token", e);
            } else if (e.getStatusCode() == 403) {
                throw new AuthorizationException("Revoke token operation rejected", e);
            } else {
                throw e;
            }
        } catch (AccessClientException e) {
            String tokenId = getTokenIdFromTokenValueSafely(tokenValue, "UNKNOWN");
            log.error("Failed to revoke token with id '{}': {}", tokenId, e.getMessage());
            log.debug("Failed to revoke token with id '{}'", tokenId, e);
            throw new RuntimeException("Failed to revoke token.", e);
        }
    }

    @Override
    public void revokeTokenById(@Nonnull String tokenId) {
        try {
            boolean found = accessClient.token().revokeById(tokenId);
            if (!found) {
                throw new TokenNotFoundException("Token not found with id: " + tokenId);
            }
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() == 404) {
                throw new TokenNotFoundException("Token not found with id: " + tokenId, e);
            } else if (e.getStatusCode() == 403) {
                throw new AuthorizationException("Revoke token operation rejected", e);
            } else {
                throw e;
            }
        } catch (AccessClientException e) {
            log.error("Failed to revoke token by id '{}': {}", tokenId, e.getMessage());
            log.debug("Failed to revoke token by id '{}'", tokenId, e);
            throw new RuntimeException("Failed to revoke token by id '" + tokenId + "'", e);
        }
    }

    @Nullable
    private String getTokenIdFromTokenValueSafely(@Nonnull String tokenValue, @Nullable String defaultValue) {
        try {
            return parseToken(tokenValue).getTokenId();
        } catch (IllegalArgumentException e) {
            log.debug("Failed to parse token value, returning default value '{}' instead of the token ID.",
                    defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Nonnull
    public JwtAccessToken parseToken(@Nonnull String tokenValue) throws IllegalArgumentException {
        requireNonNull(tokenValue, "Token value is required");
        return accessClient.token().parse(tokenValue);
    }

    @Override
    public boolean verifyToken(@Nonnull JwtAccessToken accessToken) {
        TokenVerifyResult result = verifyAndGetResult(accessToken);
        if (result.isSuccessful()) {
            return true;
        } else {
            log.debug("Token with id '{}' failed verification, reason: {}", accessToken.getTokenId(),
                    result.getReason());
        }
        return false;
    }

    @Override
    public TokenVerifyResult verifyAndGetResult(@Nonnull JwtAccessToken accessToken) {
        requireNonNull(accessToken, "Access token is required");
        try {
            return accessClient.token().verify(accessToken.getTokenValue());
        } catch (AccessClientException e) {
            String tokenId = accessToken.getTokenId();
            log.error("Failed to verify access token with id '{}': {}", tokenId, e.getMessage());
            log.debug("Failed to verify access token with id '{}'", tokenId, e);
            throw new RuntimeException("Failed to verify access token with id '" + tokenId + "'", e);
        }
    }

    @Nonnull
    @Override
    public ServiceId getArtifactoryServiceId() {
        return serviceId;
    }

    @Override
    public boolean isTokenAppliesScope(@Nonnull JwtAccessToken accessToken, @Nonnull String requiredScope) {
        //TODO [YA] this is currently enough, but will probably need to be more sophisticated in the near future...
        return accessToken.getScope().stream().anyMatch(scope -> scope.equals(requiredScope));
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        log.info("Reloading Access Service");
        configStore.loadAdminTokenFromConfig();

        // TODO [NS] Understand the true meaning of this as access reloads and invalidates the cache as part of the initAccessService process
        //if (!loaded) {
        //    // Generate a new admin token and bootstrap the client
        //    initAccessService(true);
        //}

        PasswordExpirationPolicy oldExpirationPolicy = oldDescriptor.getSecurity().getPasswordSettings()
                .getExpirationPolicy();
        PasswordExpirationPolicy expirationPolicy = centralConfigService.getDescriptor().getSecurity()
                .getPasswordSettings()
                .getExpirationPolicy();

        if (expirationPolicy == null && oldExpirationPolicy == null) {
            return;
        }

        if (expirationPolicy != null && expirationPolicy.equals(oldExpirationPolicy)) {
            return;
        }

        updateAccessConfiguration(centralConfigService.getDescriptor().getSecurity());
    }

    private void updateAccessConfiguration(SecurityDescriptor security) {
        log.info("Updating access configuration with password expiration data");
        Map<String, Object> configMap = securityToConfigMap(security);
        ConfigurationModel configurationModel = new ConfigurationModel(
                YamlUtils.getInstance().valueToString(configMap));

        RestResponse restResponse = getAccessClient().restCall(
                RestRequest.patch("/api/v1/config")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .body(JsonUtils.getInstance().valueToString(configurationModel), Charset.defaultCharset())
                        .build());

        if (!restResponse.isSuccessful()) {
            byte[] body = restResponse.getBody();
            log.error("Couldn't update access configuration, http response code: {}, body: {}",
                    restResponse.getStatusCode(), body != null ? new String(body) : "[empty body]");
        }
    }

    private Map<String, Object> securityToConfigMap(SecurityDescriptor security) {
        PasswordExpirationPolicy expirationPolicy = security.getPasswordSettings().getExpirationPolicy();
        return ImmutableMap.of("security",
                ImmutableMap.of("user-lock-policy",
                        ImmutableMap.of("password-expiry-days",
                                expirationPolicy.isEnabled() ? expirationPolicy.getPasswordMaxAge() : 0)));
    }

    @Override
    public void destroy() {
        IOUtils.closeQuietly(accessClient);
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }

    @Override
    public void ping() {
        try {
            this.accessClient.useAuth(null).ping();
        } catch (AccessClientException e) {
            throw new AccessClientException("Access service is unavailable: " + e.getMessage());
        }
    }

    @Override
    public void exportTo(ExportSettings settings) {
        if (!isUsingBundledAccessServer()) {
            log.debug("Artifactory is not using its bundled access server - skipping triggering access server export");
            return;
        }
        try {
            log.info("Triggering export in access server...");
            accessClient.useAuth(accessAdminCredentials).system().exportAccessServer();
            Path exportEtcFolder = settings.getBaseDir().toPath().resolve("etc");
            File accessServerBackupDir = new File(artifactoryHome.getBundledAccessHomeDir(), "backup");
            Files.list(accessServerBackupDir.toPath())
                    .filter(f -> !Files.isDirectory(f))
                    .filter(f -> f.toFile().getName().matches("access\\.backup\\..+\\.json"))
                    .max((f1, f2) -> (int) (f1.toFile().lastModified() - f2.toFile().lastModified()))
                    .ifPresent(path -> copyFile(path, exportEtcFolder));
        } catch (Exception e) {
            log.debug("Error during access server backup", e);
            settings.getStatusHolder().error("Error during access server backup", e, log);
        }
    }

    private Path copyFile(Path srcFile, Path targetFolder) {
        Path trgFile = null;
        try {
            Files.createDirectories(targetFolder);
            trgFile = targetFolder.resolve(ACCESS_BOOTSTRAP_JSON);
            Path destination_path = Files.copy(srcFile, trgFile, COPY_ATTRIBUTES, REPLACE_EXISTING);
            // Set permissions to 644 in order to be able to read the file on import (Access sets permission to 700)
            setPermissionsOnSecurityFile(destination_path, PERMISSIONS_MODE_644);
            return destination_path;
        } catch (IOException e) {
            String error = "Unable to copy the file from '" + srcFile + "' to '" + trgFile + "'";
            log.debug(error, e);
            throw new RuntimeException(error, e);
        }
    }

    @Override
    public void importSecurityEntities(SecurityInfo securityInfo) {
        try {
            getAccessClient().useAuth(accessAdminCredentials).imports()
                    .importSecurityEntities(buildImportRequest(securityInfo));
        } finally {
            invalidateAdminCredentials();
        }
    }

    private ImportEntitiesRequest buildImportRequest(SecurityInfo securityInfo) {
        ImportEntitiesRequest.Builder entities = ImportEntitiesRequest.builder();
        List<GroupInfo> groups = securityInfo.getGroups();
        if (groups != null) {
            for (GroupInfo group : groups) {
                entities.addGroup(GroupMapper.toFullAccessGroup(group));
            }
        }
        List<UserInfo> users = securityInfo.getUsers();
        if (users != null) {
            for (UserInfo user : users) {
                entities.addUser(UserMapper.toFullAccessUser(user));
            }
        }
        List<AclInfo> acls = securityInfo.getAcls();
        if (acls != null) {
            String serviceId = getArtifactoryServiceId().getFormattedName();
            for (AclInfo acl : acls) {
                entities.addPermission(AclMapper.toFullAccessPermission(acl, serviceId));
            }
        }
        return entities.build();
    }

    @Override
    public void importFrom(ImportSettings settings) {
        importAccessServer(settings);
    }

    @Override
    public void afterImport(ImportSettings settings) {
        initServiceId();
        initAccessService(true);
    }

    private void importAccessServer(ImportSettings settings) {
        if (!isUsingBundledAccessServer()) {
            log.debug("Artifactory is not using its bundled access server - skipping triggering access server import");
            return;
        }
        log.info("Triggering import in access server...");
        File bootstrapFile = new File(settings.getBaseDir(), "etc" + File.separator + ACCESS_BOOTSTRAP_JSON);
        if (bootstrapFile.exists()) {
            Path targetPath = new File(artifactoryHome.getBundledAccessHomeDir(),
                    "etc" + File.separator + ACCESS_BOOTSTRAP_JSON)
                    .toPath();
            boolean failed = false;
            try {
                FileUtils.forceMkdir(targetPath.toFile().getParentFile());
                Files.copy(bootstrapFile.toPath(), targetPath, COPY_ATTRIBUTES, REPLACE_EXISTING);
            } catch (IOException e) {
                failed = true;
                log.debug("Failed to import access bootstrap file: {}", bootstrapFile.getAbsolutePath(), e);
                settings.getStatusHolder()
                        .error("Failed to import access bootstrap file: " + bootstrapFile.getAbsolutePath()
                                + " Please check file permissions", e, log);
            }
            if (!failed) {
                try {
                    accessClient.useAuth(accessAdminCredentials).system().importAccessServer();
                    publisher.publishEvent(new InvalidateCacheEvent(this, CacheType.ACL));
                } catch (Exception e) {
                    log.debug("Error during access server restore", e);
                    settings.getStatusHolder().error("Error during access server restore", e, log);
                }
            }
        }
    }

    private void invalidateAdminCredentials() {
        configStore.invalidateAdminCredentials();
    }

    private boolean isUsingBundledAccessServer() {
        return configStore.isUsingBundledAccessServer();
    }

    private static class ContextStateDependantActionRunner implements ContextReadinessListener {

        private final List<Runnable> onContextCreatedActions = Lists.newArrayList();
        private boolean contextCreated = false;

        @Override
        public void onContextCreated() {
            contextCreated = true;
            onContextCreatedActions.forEach(Runnable::run);
            onContextCreatedActions.clear();
        }

        @Override
        public void onContextReady() {
            //nothing to do
        }

        public void runAfterContextCreated(Runnable action) {
            if (contextCreated) {
                action.run();
            } else {
                onContextCreatedActions.add(action);
            }
        }
    }

    void runAfterContextCreated(Runnable action) {
        contextStateDependantActionRunner.runAfterContextCreated(action);
    }

    @Override
    public void onContextCreated() {
        contextStateDependantActionRunner.onContextCreated();
    }

    @Override
    public void onContextReady() {
        contextStateDependantActionRunner.onContextReady();
    }

    private void pause() {
        try {
            sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException("Waiting for access server got interrupted.", e);
        }
    }
}

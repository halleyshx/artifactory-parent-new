package org.artifactory.security.access;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.accesstoken.AccessClientSettings;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.TomcatUtils;
import org.jfrog.access.client.AccessAuthToken;
import org.jfrog.access.client.AccessClientBuilder;
import org.jfrog.access.client.RootCertificateHolder;
import org.jfrog.access.client.confstore.AccessClientConfigStore;
import org.jfrog.access.common.ServiceId;
import org.jfrog.access.util.AccessCredsFileHelper;
import org.jfrog.access.version.AccessVersion;
import org.jfrog.config.wrappers.FileEventType;
import org.jfrog.security.crypto.EncryptionWrapper;
import org.jfrog.security.file.PemHelper;
import org.jfrog.security.file.SecurityFolderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.cert.Certificate;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jfrog.access.util.AccessCredsFileHelper.ADMIN_DEFAULT_USERNAME;
import static org.jfrog.security.file.SecurityFolderHelper.PERMISSIONS_MODE_600;

/**
 * @author Yinon Avraham.
 */
class ArtifactoryAccessClientConfigStore implements AccessClientConfigStore {

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryAccessClientConfigStore.class);
    private static final String CACHED_SERVER_URL = "serverUrl";
    private static final String CACHED_ADMIN_TOKEN = "adminToken";
    private static final String CACHED_ADMIN_CREDS = "adminCreds";

    private final Cache<String, Object> cachedValues = CacheBuilder.newBuilder().build();
    private final AccessServiceImpl accessService;
    private final InternalCentralConfigService configService;
    private final SecurityService securityService;
    private final ArtifactoryHome artifactoryHome;
    private final File rootCrtFile;
    private final File clientVersionFile;
    private final File bootstrapCredsFile;
    private final File accessAdminCredsFile;
    private ServiceId serviceId;

    ArtifactoryAccessClientConfigStore(AccessServiceImpl accessService, ServiceId serviceId) {
        this.accessService = requireNonNull(accessService, "access service is required");
        this.configService = requireNonNull(accessService.centralConfigService(), "central config service is required");
        this.securityService = requireNonNull(accessService.securityService(), "security service is required");
        this.artifactoryHome = requireNonNull(accessService.artifactoryHome(), "Artifactory home is required");
        this.rootCrtFile = new File(artifactoryHome.getAccessClientDir(), "keys/root.crt");
        this.clientVersionFile = new File(artifactoryHome.getAccessClientDir(), "data/access.version.properties");
        this.bootstrapCredsFile = new File(artifactoryHome.getAccessClientDir(), "bootstrap.creds");
        this.accessAdminCredsFile = artifactoryHome.getAccessAdminCredsFile();
        this.serviceId = requireNonNull(serviceId, "service ID is required");
        convertClientConfigFromEmbeddedAccessServerIfNeeded();
        initCachedBootstrapAdminCredentials();
    }

    private void convertClientConfigFromEmbeddedAccessServerIfNeeded() {
        try {
            // Convert the access client config from embedded server to bundled server according to the existence of the
            // admin token file.
            Path keysFolder = artifactoryHome.getAccessClientDir().toPath().resolve("keys");
            File adminTokenFile = keysFolder.resolve(serviceId + ".token").toFile();
            if (adminTokenFile.exists()) {
                log.debug("Admin token file exists: '{}', starting access client config conversion from " +
                        "embedded server to bundled server.", adminTokenFile.getAbsolutePath());
                // Create the access.creds file with default credentials (this is under the assumption that the embedded
                // access server always had these credentials)
                AccessCredsFileHelper.saveAccessCreds(accessAdminCredsFile, ADMIN_DEFAULT_USERNAME, "password");
                SecurityFolderHelper.setPermissionsOnSecurityFile(accessAdminCredsFile.toPath(), PERMISSIONS_MODE_600);
                // remove the old format of the admin token
                Files.delete(adminTokenFile.toPath());
                // Remove obsolete files
                Files.deleteIfExists(keysFolder.resolve(serviceId + ".token"));
                Files.deleteIfExists(keysFolder.resolve(serviceId + ".key"));
                Files.deleteIfExists(keysFolder.resolve(serviceId + ".crt"));
                Files.deleteIfExists(keysFolder.resolve("keystore.jks"));
            } else {
                log.debug("Admin token file does not exist: '{}', skipping access client config conversion from " +
                        "embedded server to bundled server.", adminTokenFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert access client config from embedded to bundled server.", e);
        }
    }

    @Nonnull
    @Override
    public AccessClientBuilder newClientBuilder() {
        String adminToken = getDecryptedAdminToken();
        AccessClientBuilder clientBuilder = AccessClientBuilder.newBuilder()
                .serverUrl(getAccessServerSettings().getUrl())
                .serviceId(getServiceId())
                .rootCertificate(newRootCertificateHolder())
                .defaultAuth(adminToken == null ? null : new AccessAuthToken(adminToken));
        getTokenVerifyResultCacheSize().ifPresent(clientBuilder::tokenVerificationResultCacheSize);
        getTokenVerifyResultCacheExpiry().ifPresent(clientBuilder::tokenVerificationResultCacheExpiry);
        getConnectionTimeout().ifPresent(clientBuilder::connectionTimeout);
        getSocketTimeout().ifPresent(clientBuilder::socketTimeout);
        getMaxConnections().ifPresent(clientBuilder::maxConnections);

        return clientBuilder;
    }

    private OptionalInt getMaxConnections() {
        return getClientIntNonNegativeSetting(ConstantValues.accessClientMaxConnections,
                AccessClientSettings::getMaxConnections);
    }

    private OptionalInt getSocketTimeout() {
        return getClientIntNonNegativeSetting(ConstantValues.accessClientSocketTimeout,
                AccessClientSettings::getSocketTimeout);
    }

    private OptionalInt getConnectionTimeout() {
        return getClientIntNonNegativeSetting(ConstantValues.accessClientConnectionTimeout,
                AccessClientSettings::getConnectionTimeout);
    }

    private OptionalLong getTokenVerifyResultCacheSize() {
        return getClientLongSetting(ConstantValues.accessClientTokenVerifyResultCacheSize,
                AccessClientSettings::getTokenVerifyResultCacheSize);
    }

    private OptionalLong getTokenVerifyResultCacheExpiry() {
        return getClientLongSetting(ConstantValues.accessClientTokenVerifyResultCacheExpiry,
                AccessClientSettings::getTokenVerifyResultCacheExpirySeconds);
    }

    private OptionalInt getClientIntNonNegativeSetting(ConstantValues constantValue,
            Function<AccessClientSettings, Integer> configGetter) {
        OptionalLong optionalLong = getClientLongSetting(constantValue,
                clientSettings ->
                        Optional.ofNullable(configGetter.apply(clientSettings))
                                .map(Integer::longValue).orElse(null),
                value -> value <= 0
        );

        if (optionalLong.isPresent()) {
            return OptionalInt.of(Long.valueOf(optionalLong.getAsLong()).intValue());
        }

        return OptionalInt.empty();
    }

    private OptionalLong getClientLongSetting(ConstantValues constantValue,
            Function<AccessClientSettings, Long> configGetter) {
        return getClientLongSetting(constantValue, configGetter, value -> value < 0);
    }

    private OptionalLong getClientLongSetting(ConstantValues constantValue,
            Function<AccessClientSettings, Long> configGetter,
            Predicate<Long> defaultTester) {
        long value = constantValue.getLong(artifactoryHome);
        log.debug("sys-prop value: {}={}", constantValue.getPropertyName(), value);
        if (defaultTester.test(value)) {
            AccessClientSettings clientSettings = getAccessClientSettings();
            if (clientSettings != null) {
                Long settingValue = configGetter.apply(clientSettings);
                log.debug("Client settings config: {}={}", constantValue.getPropertyName(), value);
                value = settingValue == null ? value : settingValue;
            }
        }

        return defaultTester.test(value) ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private RootCertificateHolder newRootCertificateHolder() {
        return new RootCertificateHolder() {
            @Nullable
            @Override
            public Certificate get() {
                return getRootCertificate();
            }

            @Override
            public void set(@Nullable Certificate certificate) {
                storeRootCertificate(certificate);
            }
        };
    }

    public void setServiceId(@Nonnull ServiceId serviceId) {
        this.serviceId = requireNonNull(serviceId, "service ID is required");
    }

    @Nonnull
    @Override
    public ServiceId getServiceId() {
        if (serviceId == null) {
            throw new IllegalStateException("Service ID was not set");
        }
        return serviceId;
    }

    @Override
    public void storeRootCertificate(@Nonnull Certificate certificate) {
        try {
            FileUtils.forceMkdir(rootCrtFile.getParentFile());
            PemHelper.saveCertificate(rootCrtFile, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save root.crt file to path: " + rootCrtFile.getAbsolutePath(), e);
        }
    }

    @Nonnull
    @Override
    public Certificate getRootCertificate() {
        try {
            return PemHelper.readCertificate(rootCrtFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read root certificate from file: " + rootCrtFile.getAbsolutePath(),
                    e);
        }
    }

    @Override
    public boolean isRootCertificateExists() {
        return rootCrtFile.exists();
    }

    @Override
    public void storeAdminToken(@Nonnull String tokenValue) {
        setAdminToken(tokenValue);
    }

    @Nonnull
    @Override
    public String getAdminToken() {
        return Optional.ofNullable(getDecryptedAdminToken())
                .orElseThrow(() -> new IllegalStateException("Admin token missing"));
    }

    @Override
    public boolean isAdminTokenExists() {
        return getRawAdminToken() != null;
    }

    @Nullable
    private String getDecryptedAdminToken() {
        String adminToken;
        String rawAdminToken = adminToken = getRawAdminToken();
        if (rawAdminToken != null) {
            EncryptionWrapper encryptionWrapper = artifactoryHome.getArtifactoryEncryptionWrapper();
            adminToken = encryptionWrapper.decryptIfNeeded(rawAdminToken).getDecryptedData();
        }
        return adminToken;
    }

    @Nullable
    private String getRawAdminToken() {
        AccessClientSettings clientSettings = getAccessClientSettings();
        String adminToken = clientSettings == null ? null : clientSettings.getAdminToken();
        return Optional.ofNullable(adminToken).orElse((String) cachedValues.getIfPresent(CACHED_ADMIN_TOKEN));
    }

    @Override
    public void revokeAdminToken() {
        setAdminToken(null);
    }

    private void setAdminToken(String adminToken) {
        if (adminToken == null) {
            cachedValues.invalidate(CACHED_ADMIN_TOKEN);
        } else {
            cachedValues.put(CACHED_ADMIN_TOKEN, adminToken);
        }
        accessService.runAfterContextCreated(() -> {
            if (configService.isSaveDescriptorAllowed()) {
                MutableCentralConfigDescriptor mutableDescriptor = configService.getMutableDescriptor();
                String securedToken = adminToken == null ? null :
                        artifactoryHome.getArtifactoryEncryptionWrapper().encryptIfNeeded(adminToken);
                mutableDescriptor.getSecurity().getAccessClientSettings().setAdminToken(securedToken);
                saveConfigDescriptor(mutableDescriptor);
            }
        });
    }

    void invalidateAdminCredentials() {
        cachedValues.invalidate(CACHED_ADMIN_CREDS);
    }

    /**
     * Loads the admin token from the config descriptor and caches it.
     *
     * @return true if admin token was found and loaded, false if the config descriptor did not contain admin token
     * (e.g. after import from
     */
    boolean loadAdminTokenFromConfig() {
        AccessClientSettings clientSettings = getAccessClientSettings();
        String adminToken = clientSettings == null ? null : clientSettings.getAdminToken();
        if (adminToken != null) {
            cachedValues.put(CACHED_ADMIN_TOKEN, adminToken);
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public String[] getBootstrapAdminCredentials() {
        try {
            String[] adminCreds = (String[]) cachedValues.get(CACHED_ADMIN_CREDS, () -> {
                final String credsNotFoundMessage = "bootstrap admin credentials do not exist in the config store";
                IOThrowingSupplier<Map<String, String>> credsSupplier;
                if (bootstrapCredsFile.exists()) {
                    log.debug("Found bootstrap admin ");
                    credsSupplier = () -> AccessCredsFileHelper.readAccessCreds(bootstrapCredsFile);
                } else if (accessAdminCredsFile.exists()) {
                    credsSupplier = this::readAdminCredentials;
                } else {
                    throw new NoSuchElementException(credsNotFoundMessage);
                }
                Entry<String, String> creds = credsSupplier.get().entrySet().stream()
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(credsNotFoundMessage));
                return new String[]{creds.getKey(), creds.getValue()};
            });
            return Arrays.copyOf(adminCreds, adminCreds.length);
        } catch (Exception e) {
            Throwable noSuchElementException = ExceptionUtils.getCauseOfTypes(e, NoSuchElementException.class);
            if (noSuchElementException != null) {
                throw (NoSuchElementException) noSuchElementException;
            }
            throw new RuntimeException("Failed to read admin credentials.", e);
        }
    }

    private void initCachedBootstrapAdminCredentials() {
        if (isBootstrapAdminCredentialsExist()) {
            getBootstrapAdminCredentials();
            discardBootstrapAdminCredentials();
        }
    }

    private interface IOThrowingSupplier<T> {
        T get() throws IOException;
    }

    private Map<String, String> readAdminCredentials() throws IOException {
        EncryptionWrapper encryptionWrapper = artifactoryHome.getArtifactoryEncryptionWrapper();
        String fileContent = FileUtils.readFileToString(accessAdminCredsFile);
        String accessCredsContent = encryptionWrapper.decryptIfNeeded(fileContent).getDecryptedData();
        return AccessCredsFileHelper.readAccessCreds(accessCredsContent);
    }

    @Override
    public boolean isBootstrapAdminCredentialsExist() {
        return bootstrapCredsFile.exists() || accessAdminCredsFile.exists();
    }

    @Override
    public void discardBootstrapAdminCredentials() {
        if (bootstrapCredsFile.exists()) {
            try {
                FileUtils.forceMkdir(accessAdminCredsFile.getParentFile());
                backupAccessAdminCredsFile();
                Files.move(bootstrapCredsFile.toPath(), accessAdminCredsFile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to move '" + bootstrapCredsFile.getAbsolutePath() + "' to '" +
                        accessAdminCredsFile.getAbsolutePath() + "'", e);
            }
            encryptOrDecryptAccessCreds(true);
            setPermissionsOnSecurityFile(accessAdminCredsFile, PERMISSIONS_MODE_600);
        }
    }

    void invalidateCache() {
        cachedValues.invalidateAll();
    }

    @Override
    public AccessVersion getAccessClientVersion() {
        if (clientVersionFile.exists()) {
            Properties props = new Properties();
            try (InputStream input = new FileInputStream(clientVersionFile)) {
                props.load(input);
                return AccessVersion.read(props);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read access client version from file: " +
                        clientVersionFile.getAbsolutePath(), e);
            }
        }
        return null;
    }

    @Override
    public void storeAccessClientVersion(@Nonnull AccessVersion accessVersion) {
        Properties props = new Properties();
        accessVersion.write(props);
        try {
            FileUtils.forceMkdir(clientVersionFile.getParentFile());
            try (OutputStream out = new FileOutputStream(clientVersionFile)) {
                props.store(out, "JFrog Access Client Version");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save access client version to file: " + this.clientVersionFile, e);
        }
    }

    private void setPermissionsOnSecurityFile(File file, Set<PosixFilePermission> permissions) {
        try {
            SecurityFolderHelper.setPermissionsOnSecurityFile(file.toPath(), permissions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to set permissions on file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Encrypt or decrypt the stored access credentials (if exists and if needed)
     *
     * @param encrypt flag to indicate the required action, <tt>true</tt> to encrypt, <tt>false</tt> to decrypt.
     */
    public void encryptOrDecryptAccessCreds(boolean encrypt) {
        EncryptionWrapper encryptionWrapper = artifactoryHome.getArtifactoryEncryptionWrapper();
        if (encrypt) {
            applyEncryptDecryptOnAccessCreds("encrypt", encryptionWrapper::encryptIfNeeded);
        } else {
            applyEncryptDecryptOnAccessCreds("decrypt", s -> encryptionWrapper.decryptIfNeeded(s).getDecryptedData());
        }
    }

    private void applyEncryptDecryptOnAccessCreds(String action, Function<String, String> encryptDecrypt) {
        if (accessAdminCredsFile.exists()) {
            try {
                String adminCreds = FileUtils.readFileToString(accessAdminCredsFile);
                FileUtils.write(accessAdminCredsFile, encryptDecrypt.apply(adminCreds));
                // Forcing a change to the other nodes - update file to db and propagate
                ContextHelper.get().getConfigurationManager().forceFileChanged(accessAdminCredsFile,
                        "artifactory.security.", FileEventType.MODIFY);
            } catch (IOException e) {
                log.error("Could not " + action + " access admin credentials file '" +
                        accessAdminCredsFile.getAbsolutePath() + "': " + e.toString());
                log.debug("Could not " + action + " access admin credentials file '" +
                        accessAdminCredsFile.getAbsolutePath() + "'.", e);
            } catch (Exception e) {
                log.debug("Failed to propagate access.creds change", e);
                log.warn("Failed to propagate access.creds change");
            }
        }
    }

    private void backupAccessAdminCredsFile() {
        try {
            FileUtils.copyFile(accessAdminCredsFile,
                    new File(accessAdminCredsFile.getAbsolutePath() + "." + System.currentTimeMillis() + ".bak"));
        } catch (IOException e) {
            log.debug("could not rotate access admin creds file ", e);
        }
    }

    /**
     * Check whether using the bundled access server.
     *
     * @see #getAccessServerSettings()
     */
    @Override
    public boolean isUsingBundledAccessServer() {
        return getAccessServerSettings().isBundled();
    }

    /**
     * Chooses the bundled Access Server URL to use with the following strategy:
     * <ol>
     * <li>System property {@link ConstantValues#accessClientServerUrlOverride} - can be used in tests, online, etc.
     * (NOT-BUNDLED, unless defined otherwise using {@link ConstantValues#accessServerBundled})</li>
     * <li>Config descriptor - Access Client Settings
     * (NOT-BUNDLED, unless defined otherwise using {@link ConstantValues#accessServerBundled})</li>
     * <li>Dev/test mode by default uses a spawned access standalone process with default port
     * (yields NOT-BUNDLED)</li>
     * <li>Detect port on localhost - this is the default case for production Artifactory with bundled Access
     * (yields BUNDLED)</li>
     * </ol>
     *
     * @return the Access Server URL
     *
     * @see ConstantValues#accessClientServerUrlOverride
     * @see ConstantValues#accessServerBundled
     * @see ConstantValues#test
     * @see ConstantValues#dev
     * @see ConstantValues#devHa
     */
    private AccessServerSettings getAccessServerSettings() {
        try {
            AccessServerSettings server = (AccessServerSettings) cachedValues.get(CACHED_SERVER_URL, () -> {
                BooleanSupplier accessServerBundled = () ->
                        ConstantValues.accessServerBundled.isSet(artifactoryHome) &&
                                ConstantValues.accessServerBundled.getBoolean(artifactoryHome);
                AccessServerSettings serverSettings = null;
                String serverUrl;
                //1- sys-prop with url
                log.debug("Checking for overriding server URL constant value.");
                serverUrl = ConstantValues.accessClientServerUrlOverride.getString(artifactoryHome);
                //By default the default server URL is null, only if it was set explicitly then it is used
                if (isNotBlank(serverUrl)) {
                    boolean bundled = accessServerBundled.getAsBoolean();
                    serverSettings = new AccessServerSettings(serverUrl, bundled, "system property");
                }
                //2- Config descriptor
                log.debug("Checking for Access Server URL in the config descriptor");
                serverUrl = getAccessClientSettings().getServerUrl();
                if (isNotBlank(serverUrl)) {
                    if (serverSettings == null) {
                        boolean bundled = accessServerBundled.getAsBoolean();
                        serverSettings = new AccessServerSettings(serverUrl, bundled, "config");
                    } else {
                        log.warn(
                                "*** Access Server URL is defined in both config XML ({}) and the '{}' system property ({}) ***" +
                                        "\nThis is not a healthy state - only a single method shall be defined! " +
                                        "Currently the URL from the system property will be used.", serverUrl,
                                ConstantValues.accessClientServerUrlOverride.getPropertyName(),
                                serverSettings.getUrl());
                    }
                }
                //3- dev/test mode - default url
                if (serverSettings == null) {
                    serverUrl = getDevOrTestDefaultAccessServerUrl();
                    if (isNotBlank(serverUrl)) {
                        log.debug("Running in dev/test mode - using Access server URL: {}", serverUrl);
                        serverSettings = new AccessServerSettings(serverUrl, false, "dev/test default");
                    }
                }
                //4- detect running in the same web container
                if (serverSettings == null) {
                    serverUrl = detectBundledAccessServerUrl();
                    log.debug("Detected bundled server URL: {}", serverUrl);
                    serverSettings = new AccessServerSettings(serverUrl, true, "detected");
                }
                log.info("Using Access Server URL: {}", serverSettings);
                return serverSettings;
            });
            return server;
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not get access server url and bundled mode", e);
        }
    }

    private AccessClientSettings getAccessClientSettings() {
        return configService.getDescriptor().getSecurity().getAccessClientSettings();
    }

    private void saveConfigDescriptor(MutableCentralConfigDescriptor mutableDescriptor) {
        securityService.doAsSystem(() -> configService.saveEditedDescriptorAndReload(mutableDescriptor));
    }

    private String detectBundledAccessServerUrl() {
        log.debug("Heuristically detecting bundled server URL.");
        Set<TomcatUtils.ConnectorDetails> connectors = TomcatUtils.getHttpConnectors();
        if (connectors.isEmpty()) {
            throw new IllegalStateException("Could not detect listening port.");
        }
        // prefer the http connector and port 8340 for bundled mode communication
        Optional<TomcatUtils.ConnectorDetails> httpConnector = connectors.stream()
                .filter(c -> c.getScheme().equalsIgnoreCase("http"))
                .filter(c -> c.getPort() == 8040)
                .findFirst();

        if (!httpConnector.isPresent()) {
            httpConnector = connectors.stream()
                    .filter(c -> c.getScheme().equalsIgnoreCase("http")).findFirst();
        }

        TomcatUtils.ConnectorDetails connector = httpConnector.orElseGet(() -> connectors.iterator().next());

        return connector.getScheme() + "://localhost:" + connector.getPort() + "/access";
    }

    private String getDevOrTestDefaultAccessServerUrl() {
        log.debug("Checking for dev/test server URL.");
        if (isDevOrTest()) {
            return "https://localhost:8340";
        }
        return null;
    }

    private boolean isDevOrTest() {
        return ConstantValues.test.getBoolean(artifactoryHome) ||
                ConstantValues.dev.getBoolean(artifactoryHome) ||
                ConstantValues.devHa.getBoolean(artifactoryHome);
    }


    private static class AccessServerSettings {
        private final String url;
        private final boolean bundled;
        private final String source;

        private AccessServerSettings(String url, boolean bundled, String source) {
            this.url = url;
            this.bundled = bundled;
            this.source = source;
        }

        public String getUrl() {
            return url;
        }

        public boolean isBundled() {
            return bundled;
        }

        @Override
        public String toString() {
            return url + " (" + (bundled ? "" : "not ") + "bundled) source: " + source;
        }
    }
}

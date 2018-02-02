package org.artifactory.common;

import com.google.common.collect.Lists;
import org.artifactory.common.config.db.ArtifactoryDbProperties;
import org.jfrog.config.BroadcastChannel;
import org.jfrog.config.DbChannel;
import org.jfrog.config.Home;
import org.jfrog.config.LogChannel;
import org.jfrog.config.broadcast.TemporaryBroadcastChannelImpl;
import org.jfrog.config.db.CommonDbProperties;
import org.jfrog.config.db.TemporaryDBChannel;
import org.jfrog.config.log.PermanentLogChannel;
import org.jfrog.config.log.TemporaryLogChannel;
import org.jfrog.config.wrappers.*;
import org.jfrog.security.util.Pair;
import org.jfrog.storage.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.artifactory.common.ArtifactoryHome.*;

/**
 * @author gidis
 */
public class ArtifactoryConfigurationAdapter implements ConfigurationManagerAdapter {

    protected DbChannel dbChannel;
    protected LogChannel logChannel;
    protected BroadcastChannel broadcastChannel;
    protected boolean ha;
    protected boolean primary;
    protected final ArtifactoryHome home;
    private final Object dbChannelLock = new Object();

    private final List<MetaInfFile> defaultConfigs;
    private final List<SharedConfigMetadata> sharedConfigs;
    private final List<SharedFolderMetadata> sharedFolders;
    //TODO [by dan]: security folder MUST be whitelist-based!!!!!!!!!!
    private final static List<String> blackList = Lists.newArrayList(
            "artifactory.security.binstore",
            "artifactory.security." + COMMUNICATION_KEY_FILE_NAME,
            "artifactory.security." + COMMUNICATION_KEY_FILE_NAME + ".old",
            "artifactory.security." + COMMUNICATION_TOKEN_FILE_NAME,
            "artifactory.security." + ARTIFACTORY_KEY_DEFAULT_TEMP_FILE_NAME,
            "artifactory.security.master.key",
            "artifactory.service_id",
            "artifactory.config.xml",
            "artifactory.security.access"
    );

    /**
     * Initialize JavaFilesWatcher and register the shared files in order to receive events on file changes and then
     * synchronize the changes with the database and the other nodes
     */
    public ArtifactoryConfigurationAdapter(Home home) {
        this.home = (ArtifactoryHome) home;
        this.defaultConfigs = initDefaultConfigs();
        this.sharedConfigs = initSharedConfigs();
        this.sharedFolders = initSharedFolders();
    }

    private List<MetaInfFile> initDefaultConfigs() {
        return Lists.newArrayList(
                new MetaInfFile("/META-INF/default/" + MIME_TYPES_FILE_NAME, home.getMimeTypesFile()),
                new MetaInfFile("/META-INF/default/" + ARTIFACTORY_SYSTEM_PROPERTIES_FILE, home.getArtifactorySystemPropertiesFile()),
                new MetaInfFile("/META-INF/default/" + BINARY_STORE_FILE_NAME, home.getBinaryStoreXmlFile()),
                new MetaInfFile("/META-INF/default/" + LOGBACK_CONFIG_FILE_NAME, home.getLogbackConfig())
        );
    }

    private List<SharedConfigMetadata> initSharedConfigs() {
        List<SharedConfigMetadata> sharedConfigs = Lists.newArrayList(
                // Artifactory system properties
                new SharedConfigMetadata(home.getArtifactorySystemPropertiesFile(), "artifactory.system.properties",
                        "/META-INF/default/" + ARTIFACTORY_SYSTEM_PROPERTIES_FILE,
                        true, false, false),
                // mimetypes.xml
                new SharedConfigMetadata(home.getMimeTypesFile(), "artifactory.mimeType",
                        "/META-INF/default/" + MIME_TYPES_FILE_NAME,
                        true, false, false),
                // binarystore.xml
                new SharedConfigMetadata(home.getBinaryStoreXmlFile(), "artifactory.binarystore.xml",
                        "/META-INF/default/" + BINARY_STORE_FILE_NAME,
                        true, true, false),
                // Artifactory encryption key
                new SharedConfigMetadata(home.getArtifactoryKey(), "artifactory.security.artifactory.key",
                        null, false, true, false),
                // Access creds
                new SharedConfigMetadata(home.getAccessAdminCredsFile(),
                        "artifactory.security.access/keys/access.creds",
                        null, false, true, false)
        );
        if (home.isHaConfigured()) {
            // Artifactory cluster License
            sharedConfigs.add(new SharedConfigMetadata(home.getLicenseFile(), "artifactory.cluster.license",
                    null, false, true, false));
        }
        return sharedConfigs;
    }

    private List<SharedFolderMetadata> initSharedFolders() {
        return Lists.newArrayList(
                // Plugins dir
                new SharedFolderMetadata(home.getPluginsDir(), "artifactory.plugin.", false, false),
                // UI logo dir
                new SharedFolderMetadata(home.getLogoDir(), "artifactory.ui.", false, false),
                // Security dir //TODO [by dan]: security folder MUST be whitelist-based!!!!!!!!!!
                new SharedFolderMetadata(home.getSecurityDir(), "artifactory.security.", true, false)
        );
    }

    @Override
    public void initialize() {
        this.home.initArtifactorySystemProperties();
        //getBooleanProperty does not explode.
        this.logChannel = new TemporaryLogChannel(home.getArtifactoryProperties().getBooleanProperty(ConstantValues.bootstrapLoggerDebug));
        this.broadcastChannel = new TemporaryBroadcastChannelImpl();
        this.primary = home.getArtifactoryHaNodePropertiesFile().exists() && home.getHaNodeProperties() != null
                && home.getHaNodeProperties().isPrimary();
        this.ha = home.getArtifactoryHaNodePropertiesFile().exists();
    }

    @Override
    public List<String> getBlackListConfigs() {
        return blackList;
    }

    @Override
    public List<MetaInfFile> getDefaultConfigs() {
        return defaultConfigs;
    }

    /**
     * The method register the shared files in the JavaFilesWatcher to receive file change on the files
     */
    @Override
    public List<SharedConfigMetadata> getSharedConfigs() {
        return sharedConfigs;
    }

    @Override
    public List<SharedFolderMetadata> getSharedFolders() {
        return sharedFolders;
    }

    @Override
    public Home getHome() {
        return home;
    }

    @Override
    public void unbind() {
        ArtifactoryHome.unbind();
    }

    @Override
    public void bind() {
        ArtifactoryHome.bind(home);
    }

    @Override
    public boolean allowDbUpdate() {
        return !ha || primary;
    }

    @Override
    public LogChannel getLogChannel() {
        return logChannel;
    }

    @Override
    public DbChannel getDbChannel() {
        if (dbChannel == null) {
            synchronized (dbChannelLock) {
                if (dbChannel == null) {
                    ArtifactoryDbProperties dbProperties = this.home.initDBProperties();
                    blockIfHAWithDerby(dbProperties);
                    this.dbChannel = new TemporaryDBChannel(new CommonDbProperties(dbProperties.getPassword(),
                            dbProperties.getConnectionUrl(), dbProperties.getUsername(), dbProperties.getDbType(),
                            dbProperties.getDriverClass()));
                }
            }
        }
        return dbChannel;
    }

    @Override
    public BroadcastChannel getBroadcastChannel() {
        return broadcastChannel;
    }

    @Override
    public void destroy() {
        if (broadcastChannel != null) {
            broadcastChannel.destroy();
        }
        if (dbChannel != null) {
            dbChannel.close();
        }
    }

    @Override
    public String getName() {
        return home.getHaAwareHostId();
    }

    /**
     * The method replaces the initial log channel into the permanent implementation
     */
    @Override
    public void setPermanentLogChannel() {
        // Replace log channel
        Logger logger = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
        logChannel = new PermanentLogChannel(logger);
    }

    /**
     * The method replaces the initial broadcast channel into the permanent implementation
     */
    @Override
    public void setPermanentBroadcastChannel(BroadcastChannel broadcastChannel) {
        // Make sure that this can be called only once
        if (this.broadcastChannel instanceof TemporaryBroadcastChannelImpl) {
            // Replace broadcastChannel but first fire accumulated events.
            TemporaryBroadcastChannelImpl initialBroadcastChannel = (TemporaryBroadcastChannelImpl) this.broadcastChannel;
            // Replace broadcastChannel
            this.broadcastChannel = broadcastChannel;
            // Fire accumulated notifications events
            Set<Pair<String, FileEventType>> notifications = initialBroadcastChannel.getNotifications();
            notifications.forEach(pair -> broadcastChannel.notifyConfigChanged(pair.getFirst(), pair.getSecond()));
        } else {
            // Can reach here only on reload
            this.broadcastChannel = broadcastChannel;
        }
    }

    /**
     * The method replaces the initial db channel into the permanent implementation
     */
    @Override
    public void setPermanentDBChannel(DbChannel permanentDbChannel) {
        try {
            DbChannel tempDBChanel = dbChannel;
            dbChannel = permanentDbChannel;
            // Replace DB channel
            if (tempDBChanel instanceof TemporaryDBChannel) {
                getLogChannel().info("Replacing temporary DB channel with permanent DB channel");
                tempDBChanel.close();
                getLogChannel().info("Successfully closed temporary DB channel");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to replace temporary db channel with the permanent one due to: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public int getRetryAmount() {
        return ConstantValues.configurationManagerRetryAmount.getInt();
    }

    // except from devenv, it is not allowed to use HA with derby
    private void blockIfHAWithDerby(ArtifactoryDbProperties dbProperties) {
        if (DbType.DERBY == dbProperties.getDbType() && home.isHaConfigured() && !ConstantValues.devHa.getBoolean()) {
            throw new RuntimeException("Cannot use Derby as the database type in HA mode, please check "
                    + home.getDBPropertiesFile().getAbsolutePath());
        }
    }
}

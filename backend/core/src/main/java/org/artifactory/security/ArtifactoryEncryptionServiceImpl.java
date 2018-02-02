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

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.layout.EncryptConfigurationInterceptor;
import org.artifactory.security.access.AccessService;
import org.artifactory.security.interceptor.ApiKeysEncryptor;
import org.artifactory.security.interceptor.StoragePropertiesEncryptInterceptor;
import org.artifactory.security.interceptor.UserPasswordEncryptor;
import org.artifactory.security.log.AuditLogger;
import org.artifactory.storage.db.mbean.NewDbInstallation;
import org.jfrog.config.wrappers.FileEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Implementation of the master encryption service based on symmetric key and Base58 encoding.
 *
 * We perform encrypt on first Artifactory startup.
 *
 * @author Yossi Shaul
 */
@Service
public class ArtifactoryEncryptionServiceImpl implements ArtifactoryEncryptionService, ApplicationListener<NewDbInstallation> {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryEncryptionServiceImpl.class);

    private boolean shouldEncryptOnStartup = false;

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private ApiKeysEncryptor apiKeysEncryptor;

    @Autowired
    private UserPasswordEncryptor userPasswordEncryptor;

    @Autowired
    private AccessService accessService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuditLogger auditLog;

    /**
     * The reason for the encryption to be in contextCreated and not on ReloadableBean.init() is HA.
     * The HA state and propagation service is not yet ready on bean init.
     * (They HA state is ready after onContextReady() method of ArtifactoryStateManagerImpl)
     */
    @Override
    public void onContextCreated() {
        // If its HA primary node OR non-HA
        if (isNonHaOrConfiguredPrimary()) {
            // Encrypt Artifactory if its the first startup
            encryptArtifactoryOnFirstStartup();
            // Ensure external files are encrypted in case a master key exists
            encryptExternalFilesIfNeeded();
        }
    }

    @Override
    public void onContextReady() {

    }

    @Override
    public void onApplicationEvent(NewDbInstallation event) {
        shouldEncryptOnStartup = true;
    }

    /**
     * Will encrypt Artifactory in case its first login AND (on HA-primary node or non-HA).
     * Encryption is creating a master key (and propagating to other HA nodes if configured).
     * At this point we don't need to encrypt the config since there are no credentials in it YET.
     * Once we create the master key - any change in the descriptor will trigger EncryptConfigurationInterceptor
     *
     * isFirstLogin is based on USERS tables in DB - in case there is no "last_login_time" record - returns true.
     * We also check that the config descriptor has only one repository, and that is the example repo, and its empty.
     */
    private void encryptArtifactoryOnFirstStartup() {
        if (shouldEncryptOnStartup) {
            log.info("Starting encryption for first Artifactory startup");
            createArtifactoryKeyIfNeeded();
            // Encrypting Access accessAdminCredsFile
            accessService.encryptOrDecrypt(true);
            shouldEncryptOnStartup = false;
        }
    }

    /**
     * In case Master Key exists we ensure the external files (db.properties) are encrypted
     * We also encrypt even though its not first startup since there might have been a change to the external files,
     * meaning someone changed the db.properties to new credentials in plain text even though Artifactory is encrypted.
     */
    private void encryptExternalFilesIfNeeded() {
        ArtifactoryHome home = ArtifactoryHome.get();
        // A master.key already exist or just been created, ensure external files are encrypted.
        if (home.getMasterKeyFile().exists()) {
            // Encrypting the storage if needed
            new StoragePropertiesEncryptInterceptor().encryptOrDecryptStoragePropertiesFile(true);
        }
    }

    /**
     * @return TRUE if Artifactory is (HA and Primary) or (Non-HA)
     */
    private boolean isNonHaOrConfiguredPrimary() {
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        return (haCommonAddon.isPrimary());
    }

    /**
     * Order is <b>VERY</b> important here, don't mess it up!
     */
    @Override
    public void encrypt() {
        AccessLogger.approved("Encrypting with master encryption key");
        // Create the master key if needed
        createArtifactoryKeyIfNeeded();
        accessService.encryptOrDecrypt(true);
        apiKeysEncryptor.encryptOrDecrypt(true);
        userPasswordEncryptor.encryptOrDecrypt(true);
        new StoragePropertiesEncryptInterceptor().encryptOrDecryptStoragePropertiesFile(true);
        // config interceptor will encrypt the config before it is saved to the database
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
        auditLog.configurationEncrypted();
    }

    private void createArtifactoryKeyIfNeeded() {
        ArtifactoryHome home = ArtifactoryHome.get();
        if (!CryptoHelper.hasArtifactoryKey(home)) {
            CryptoHelper.createArtifactoryKeyFile(home);
            notifyArtifactoryKeyCreated();
        }
    }

    /**
     * Order is <b>VERY</b> important here, don't mess it up!
     */
    @Override
    public void decrypt() {
        if (!CryptoHelper.hasArtifactoryKey(ArtifactoryHome.get())) {
            throw new IllegalStateException("Cannot decrypt without master key file");
        }
        AccessLogger.approved("Decrypting with master encryption key");
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        accessService.encryptOrDecrypt(false);
        apiKeysEncryptor.encryptOrDecrypt(false);
        userPasswordEncryptor.encryptOrDecrypt(false);
        new StoragePropertiesEncryptInterceptor().encryptOrDecryptStoragePropertiesFile(false);
        EncryptConfigurationInterceptor.decrypt(mutableDescriptor);
        File oldKeyFile = CryptoHelper.getArtifactoryKey(ArtifactoryHome.get());
        File renamedKeyFile = CryptoHelper.removeArtifactoryKeyFile(ArtifactoryHome.get());
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
        notifyArtifactoryKeyDeleted(oldKeyFile, renamedKeyFile);
        auditLog.configurationDecrypted();
    }

    @Override
    public void notifyArtifactoryKeyCreated() {
        propagateMasterKeyFileChange(ArtifactoryHome.get().getArtifactoryKey(),  FileEventType.CREATE);
        addonsManager.addonByType(HaCommonAddon.class).propagateArtifactoryEncryptionKeyChanged();
    }

    // Order is important, don't touch me!
    @Override
    public void notifyArtifactoryKeyDeleted(File oldKeyFile, File renamedKeyFile) {
        propagateMasterKeyFileChange(oldKeyFile, FileEventType.DELETE);
        propagateMasterKeyFileChange(renamedKeyFile, FileEventType.CREATE);
        addonsManager.addonByType(HaCommonAddon.class).propagateArtifactoryEncryptionKeyChanged();
    }

    private void propagateMasterKeyFileChange(File masterKey, FileEventType eventType) {
        try {
            ContextHelper.get().getConfigurationManager().forceFileChanged(masterKey,"artifactory.security.", eventType);
        } catch (Exception e) {
            log.error("Failed to propagate master key file change", e);
        }
    }
}

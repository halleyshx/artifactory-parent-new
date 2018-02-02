package org.artifactory.configuration;

import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.configuration.helper.EnvContext;
import org.artifactory.configuration.helper.TestEnvContextBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.artifactory.configuration.helper.BasicConfigurationManagerTestHelper.*;
import static org.artifactory.configuration.helper.FileCreationStage.afterHome;
import static org.artifactory.configuration.helper.FileCreationStage.beforeHome;


/**
 * @author gidis
 */
@Test
public class BasicConfigurationManagerTest {

    @AfterMethod
    public void cleanAfterCreateEnv() {
        System.setProperty(ConstantValues.masterKeyWaitingTimeout.getPropertyName(), "1000");
        ArtifactoryContextThreadBinder.unbind();
        ArtifactoryHome.unbind();
    }

    @Test
    public void timeoutWaitingForMasterKey() throws IOException {
        // Expecting BasicConfiguration to wait until timeout
        try {
            EnvContext envContext = TestEnvContextBuilder.create().build();
            createEnvironment(envContext);
            assertMasterKeyExist(envContext);
            defaultFileExists(envContext);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("master.key file is missing - timed out while waiting for master.key after"));
        }
    }

    @Test
    public void cleanStartWithMasterKey() throws IOException {
        // Expecting successful BasicConfiguration start (master.key exist)
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        // Setting master.key AFTER_HOME creation to simulate Access (providing the master.key file)
        builder.includeMasterKey(afterHome);
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
    }

    @Test
    public void masterKeyChecksumMatch() throws IOException {
        // Expecting BasicConfiguration to fail because of master key checksum mismatch
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        // Setting master.key AFTER_HOME creation to simulate Access (providing the master.key file)
        builder.includeMasterKeyInDb();
        builder.includeMasterKey(afterHome);
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Master key checksum mismatch")
    public void masterKeyChecksumMismatch() throws IOException {
        // Expecting BasicConfiguration to fail because of master key checksum mismatch
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        // Setting master.key AFTER_HOME creation to simulate Access (providing the master.key file)
        builder.includeMasterKeyInDb();
        builder.includeMasterKey2(afterHome);
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
        Assert.fail();
    }

    @Test
    public void startExistingServerWithAllConfigFiles() throws IOException {
        // Expecting successful BasicConfiguration start (master.key exist and all other configs are valid)
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        builder.includeMasterKey(beforeHome);
        builder.includeDbProperties(beforeHome);
        builder.includeArtifactorySystemProperties(beforeHome);
        builder.includeArtifactoryLogback(beforeHome);
        builder.includeArtifactoryMimeTypes(beforeHome);
        builder.includeArtifactoryProperties(beforeHome);
        builder.includeArtifactoryKey(beforeHome);
        builder.includeArtifactoryServiceId(beforeHome);
        builder.includeArtifactoryRootCert(beforeHome);
        builder.includeArtifactoryBinarystore(beforeHome);
        builder.includeAccessDbProperties(beforeHome);
        builder.includeAccessLogback(beforeHome);
        builder.includeAccessPrivate(beforeHome);
        builder.includeAccessRootCrt(beforeHome);
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
    }

    @Test
    public void startServerWithCorruptedDbProperties() throws IOException {
        // Expecting BasicConfiguration to fail (master.key exist but db.properties is corrupted)
        try {
            TestEnvContextBuilder builder = TestEnvContextBuilder.create();
            // Setting corrupted db.properties
            builder.includeCorruptedDbProperties(beforeHome);
            // Setting valid configuration files
            builder.includeMasterKey(beforeHome);
            builder.includeArtifactorySystemProperties(beforeHome);
            builder.includeArtifactoryLogback(beforeHome);
            builder.includeArtifactoryMimeTypes(beforeHome);
            builder.includeArtifactoryProperties(beforeHome);
            builder.includeArtifactoryKey(beforeHome);
            builder.includeArtifactoryServiceId(beforeHome);
            builder.includeArtifactoryRootCert(beforeHome);
            builder.includeAccessDbProperties(beforeHome);
            builder.includeArtifactoryBinarystore(beforeHome);
            builder.includeAccessLogback(beforeHome);
            builder.includeAccessPrivate(beforeHome);
            builder.includeAccessRootCrt(beforeHome);
            EnvContext envContext = builder.build();
            createEnvironment(envContext);
            assertMasterKeyExist(envContext);
            defaultFileExists(envContext);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to load artifactory DB properties from"));
        }
    }

    @Test
    public void testProtectedUploadOnEmptyDb() throws IOException {
        // Expecting BasicConfiguration to delete artifactory.file since it is protected
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        // Setting corrupted root.crt
        builder.includeMasterKey(beforeHome);
        builder.includeCorruptedArtifactoryKey(beforeHome);
        builder.includeConfigTable();
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
        assertCorruptedArtifactoryKeyExist(envContext);
    }

    //TODO [by shayb]: replace with real test that test real protected file
    //@Test
    //public void testProtectedDownloaded() throws IOException {
    //    // Expecting BasicConfiguration to download artifactory.key from DB
    //    TestEnvContextBuilder builder = TestEnvContextBuilder.create();
    //    // Setting corrupted root.crt
    //    builder.includeMasterKey(beforeHome);
    //    builder.includeConfigTable();
    //    builder.includeArtifactoryKeyInDb();
    //    EnvContext envContext = builder.build();
    //    createEnvironment(envContext);
    //    assertMasterKeyExist(envContext);
    //    defaultFileExists(envContext);
    //    assertValidArtifactoryKeyExist(envContext);
    //}

    //@Test
    //public void testProtectedOverrided() throws IOException {
    //    // Expecting BasicConfiguration to download artifactory.key from DB
    //    TestEnvContextBuilder builder = TestEnvContextBuilder.create();
    //    // Setting corrupted root.crt
    //    builder.includeMasterKey(beforeHome);
    //    builder.includeConfigTable();
    //    builder.includeArtifactoryKeyInDb();
    //    builder.includeCorruptedArtifactoryKey(beforeHome);
    //    EnvContext envContext = builder.build();
    //    createEnvironment(envContext);
    //    assertMasterKeyExist(envContext);
    //    defaultFileExists(envContext);
    //    assertValidArtifactoryKeyExist(envContext);
    //}

    @Test
    public void testProtectedImport() throws IOException {
        // Expecting BasicConfiguration to download artifactory.key from DB
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        // Setting corrupted root.crt
        builder.includeMasterKey(beforeHome);
        builder.includeConfigTable();
        builder.includeArtifactoryKeyInDb();
        builder.includeImportArtifactoryKey(beforeHome);
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        assertMasterKeyExist(envContext);
        defaultFileExists(envContext);
    }

    @Test
    public void testNoPullWithoutMasterKey() throws IOException, InterruptedException {
        TestEnvContextBuilder builder = TestEnvContextBuilder.create();
        builder.includeMasterKey(beforeHome);
        builder.includeConfigTable();
        builder.removeMasterEncryptionWrapper();
        builder.touchArtifactoryBinarystore();
        EnvContext envContext = builder.build();
        createEnvironment(envContext);
        // No master.key
        assertMasterKeyNotExists(envContext);
        // Our binarystore.xml file shouldn't have changed
        assertBinaryStoreFileNotModified(envContext, 0);
        assertValidBinaryStoreFileExists(envContext);
    }
}

package org.artifactory.converter.nonfs;

import org.artifactory.test.TestUtils;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;

import static org.artifactory.converter.helpers.ConvertersManagerTestHelper.*;
import static org.artifactory.version.ArtifactoryVersion.v4111;

/**
 * @author nadavy
 */
abstract class NoNfsConverterTestBase {

    File home;

    @BeforeClass
    public void init() throws IOException {
        home = TestUtils.createTempDir(getClass());
        createHomeEnvironment(home, v4111);
    }

    void createHaEnvironment(File home) throws IOException {
        createHaPluginsFile(home);
        createHaUiFile(home);
        createHaBackupFile(home);
        createArtifactorySystemPropertiesFile(home);
        createBinaryStoreXmlFile(home);
        createMimeTypeXmlFile(home);
        createStoragePropertiesXmlFile(home);
        createSshKeys(home);
    }
}

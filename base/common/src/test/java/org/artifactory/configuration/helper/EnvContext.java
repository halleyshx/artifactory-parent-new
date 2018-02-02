package org.artifactory.configuration.helper;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.test.TestUtils;
import org.artifactory.webapp.servlet.BasicConfigManagers;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author gidis
 */
public class EnvContext {
    private File homeDir;
    private Map<FileCreationStage, FileMetaData> dbProperties;
    private Map<FileCreationStage, FileMetaData> masterKey;
    private Map<FileCreationStage, FileMetaData> artifactorySystemProperties;
    private Map<FileCreationStage, FileMetaData> artifactoryKey;
    private Map<FileCreationStage, FileMetaData> artifactoryBinarystorexXml;
    private Map<FileCreationStage, FileMetaData> artifactoryProperties;
    private Map<FileCreationStage, FileMetaData> artifactoryLogbackXml;
    private Map<FileCreationStage, FileMetaData> artifactoryMimetypes;
    private Map<FileCreationStage, FileMetaData> artifactoryServiceId;
    private Map<FileCreationStage, FileMetaData> artifactoryRootCrt;
    private Map<FileCreationStage, FileMetaData> accessPrivate;
    private Map<FileCreationStage, FileMetaData> accessRootCert;
    private Map<FileCreationStage, FileMetaData> accessLogback;
    private Map<FileCreationStage, FileMetaData> accessDbProperties;
    private List<ConfigQueryMetData> blobQueries;
    private String masterKeyDBValue;
    private boolean createConfigTable;
    private ArtifactoryHome artifactoryHome;
    private BasicConfigurationManagerTestHelper.MockArtifactoryConfigurationAdapter adapter;
    private BasicConfigManagers basicConfigManagers;
    private List<FileMetaData> modifiedFiles;
    private boolean removeEncryptionWrapper;

    EnvContext() {
        this.homeDir = new File(TestUtils.createTempDir(getClass())+"/.artifactory");
    }

    public ArtifactoryHome getArtifactoryHome() {
        return artifactoryHome;
    }

    public void setArtifactoryHome(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
    }

    BasicConfigurationManagerTestHelper.MockArtifactoryConfigurationAdapter getAdapter() {
        return adapter;
    }

    void setMockConfigurationAdapter(BasicConfigurationManagerTestHelper.MockArtifactoryConfigurationAdapter
            mockArtifactoryConfigurationAdapter) {
        this.adapter = mockArtifactoryConfigurationAdapter;
    }

    void setBasicConfigManagers(BasicConfigManagers basicConfigManagers) {
        this.basicConfigManagers = basicConfigManagers;
    }

    public BasicConfigManagers getBasicConfigManagers() {
        return basicConfigManagers;
    }

    File getHomeDir() {
        return homeDir;
    }

    public List<ConfigQueryMetData> getBlobQueries() {
        return blobQueries;
    }

    FileMetaData getDbProperties(FileCreationStage stage) {
        return dbProperties.get(stage);
    }

    FileMetaData getMasterKey(FileCreationStage stage) {
        return masterKey.get(stage);
    }

    FileMetaData getArtifactorySystemProperties(FileCreationStage stage) {
        return artifactorySystemProperties.get(stage);
    }

    FileMetaData getArtifactoryKey(FileCreationStage stage) {
        return artifactoryKey.get(stage);
    }

    FileMetaData getArtifactoryBinarystorexXml(FileCreationStage stage) {
        return artifactoryBinarystorexXml.get(stage);
    }

    FileMetaData getArtifactoryProperties(FileCreationStage stage) {
        return artifactoryProperties.get(stage);
    }

    FileMetaData getArtifactoryLogbackXml(FileCreationStage stage) {
        return artifactoryLogbackXml.get(stage);
    }

    FileMetaData getArtifactoryMimetypes(FileCreationStage stage) {
        return artifactoryMimetypes.get(stage);
    }

    FileMetaData getArtifactoryServiceId(FileCreationStage stage) {
        return artifactoryServiceId.get(stage);
    }

    FileMetaData getArtifactoryRootCert(FileCreationStage stage) {
        return artifactoryRootCrt.get(stage);
    }

    FileMetaData getAccessPrivate(FileCreationStage stage) {
        return accessPrivate.get(stage);
    }

    FileMetaData getAccessRootCert(FileCreationStage stage) {
        return accessRootCert.get(stage);
    }

    FileMetaData getAccessLogback(FileCreationStage stage) {
        return accessLogback.get(stage);
    }

    FileMetaData getAccessDbProperties(FileCreationStage stage) {
        return accessDbProperties.get(stage);
    }


    public void setDbProperties(Map<FileCreationStage, FileMetaData> dbProperties) {
        this.dbProperties = dbProperties;
    }

    public void setArtifactorySystemProperties(Map<FileCreationStage, FileMetaData> artifactorySystemProperties) {
        this.artifactorySystemProperties = artifactorySystemProperties;
    }

    public void setArtifactoryKey(Map<FileCreationStage, FileMetaData> artifactoryKey) {
        this.artifactoryKey = artifactoryKey;
    }

    public void setArtifactoryBinarystorexXml(Map<FileCreationStage, FileMetaData> artifactoryBinarystorexXml) {
        this.artifactoryBinarystorexXml = artifactoryBinarystorexXml;
    }

    public void setArtifactoryProperties(Map<FileCreationStage, FileMetaData> artifactoryProperties) {
        this.artifactoryProperties = artifactoryProperties;
    }

    public void setArtifactoryLogbackXml(Map<FileCreationStage, FileMetaData> artifactoryLogbackXml) {
        this.artifactoryLogbackXml = artifactoryLogbackXml;
    }

    public void setArtifactoryMimetypes(Map<FileCreationStage, FileMetaData> artifactoryMimetypes) {
        this.artifactoryMimetypes = artifactoryMimetypes;
    }

    public void setArtifactoryServiceId(Map<FileCreationStage, FileMetaData> artifactoryServiceId) {
        this.artifactoryServiceId = artifactoryServiceId;
    }

    public void setAccessPrivate(Map<FileCreationStage, FileMetaData> accessPrivate) {
        this.accessPrivate = accessPrivate;
    }

    public void setAccessRootCert(Map<FileCreationStage, FileMetaData> accessRootCert) {
        this.accessRootCert = accessRootCert;
    }

    public void setAccessLogback(Map<FileCreationStage, FileMetaData> accessLogback) {
        this.accessLogback = accessLogback;
    }

    public void setAccessDbProperties(Map<FileCreationStage, FileMetaData> accessDbProperties) {
        this.accessDbProperties = accessDbProperties;
    }

    void setMasterKey(Map<FileCreationStage, FileMetaData> masterKey) {
        this.masterKey = masterKey;
    }

    public void setArtifactoryRootCrt(Map<FileCreationStage, FileMetaData> artifactoryRootCrt) {
        this.artifactoryRootCrt = artifactoryRootCrt;
    }

    public boolean isCreateHome() {
        return dbProperties.keySet().contains(FileCreationStage.beforeHome)||
        masterKey.keySet().contains(FileCreationStage.beforeHome)||
        artifactorySystemProperties.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryKey.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryBinarystorexXml.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryProperties.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryLogbackXml.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryMimetypes.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryServiceId.keySet().contains(FileCreationStage.beforeHome)||
        artifactoryRootCrt.keySet().contains(FileCreationStage.beforeHome)||
        accessPrivate.keySet().contains(FileCreationStage.beforeHome)||
        accessRootCert.keySet().contains(FileCreationStage.beforeHome)||
        accessLogback.keySet().contains(FileCreationStage.beforeHome)||
        accessDbProperties.keySet().contains(FileCreationStage.beforeHome);
    }

    public void setBlobQueries(List<ConfigQueryMetData> blobQueries) {
        this.blobQueries = blobQueries;
    }

    public void setMasterKeyDBValue(String masterKeyDBValue) {
        this.masterKeyDBValue = masterKeyDBValue;
    }

    public String getMasterKeyDBValue() {
        return masterKeyDBValue;
    }

    public void setCreateConfigTable(boolean createConfigTable) {
        this.createConfigTable = createConfigTable;
    }

    public boolean isCreateConfigTable() {
        return createConfigTable;
    }

    public void setModifiedFiles(List<FileMetaData> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    List<FileMetaData> getModifiedFiles() {
        return modifiedFiles;
    }

    void setRemoveEncryptionWrapper(boolean removeEncryptionWrapper) {
        this.removeEncryptionWrapper = removeEncryptionWrapper;
    }

    boolean isRemoveEncryptionWrapper() {
        return removeEncryptionWrapper;
    }
}

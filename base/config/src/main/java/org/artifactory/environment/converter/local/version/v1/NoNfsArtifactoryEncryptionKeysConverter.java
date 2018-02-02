package org.artifactory.environment.converter.local.version.v1;

import com.google.common.collect.Lists;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.environment.converter.shared.version.v1.NoNfsBasicEnvironmentConverter;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.security.wrapper.ArtifactoryEncryptionKeyFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.jfrog.config.wrappers.ConfigurationManagerAdapter.normalizedFilesystemPath;

/**
 * @author Dan Feldman
 */
public class NoNfsArtifactoryEncryptionKeysConverter extends NoNfsBasicEnvironmentConverter {
    private static final Logger log = LoggerFactory.getLogger(NoNfsArtifactoryEncryptionKeysConverter.class);

    private List<File> convertedFiles = Lists.newArrayList();

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        return isUpgradeTo5x(source, target);
    }

    @Override
    protected void doConvert(ArtifactoryHome home, File clusterHomeDir) {
        getConvertedFiles(home, clusterHomeDir);
        if (!convertedFiles.isEmpty()) {
            log.info("Starting environment conversion: master encryption key");
            File securityDir = home.getSecurityDir();
            for (File masterKey : convertedFiles) {
                safeCopyFile(masterKey, new File(securityDir, masterKey.getName()));
            }
            log.info("Finished environment conversion: master encryption key");
        }
    }

    @Override
    protected void doAssertConversionPreconditions(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        getConvertedFiles(artifactoryHome, clusterHomeDir);
        convertedFiles.forEach(this::assertFilePermissions);
    }

    private void getConvertedFiles(ArtifactoryHome artifactoryHome, File clusterHomeDir) {
        if (convertedFiles.isEmpty()) {
            if (clusterHomeDir != null && clusterHomeDir.exists()) {
                File etcSecurity = new File(clusterHomeDir, normalizedFilesystemPath("ha-etc", "security"));
                File[] masterKeys = etcSecurity.listFiles(
                        new ArtifactoryEncryptionKeyFileFilter(ConstantValues.securityArtifactoryKeyLocation.getString(artifactoryHome)));
                if (masterKeys != null && masterKeys.length > 0) {
                    Collections.addAll(convertedFiles, masterKeys);
                }
            }
        }
    }
}

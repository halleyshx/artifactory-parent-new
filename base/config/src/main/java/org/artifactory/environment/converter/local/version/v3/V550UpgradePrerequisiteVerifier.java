package org.artifactory.environment.converter.local.version.v3;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.converter.ConverterPreconditionException;
import org.artifactory.environment.converter.BasicEnvironmentConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.common.logging.BootstrapLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Simply put, we prevent HA upgrades from versions < 5.4.6 to > 5.5.0 --> users must upgrade to 5.4.6 before upgrading
 * to 5.5.0
 * For pro installations any upgrade is still possible since downtime (caused by non-compatible dao) will not occur.
 * @author Dan Feldman
 */
public class V550UpgradePrerequisiteVerifier implements BasicEnvironmentConverter {
    private static final Logger log = LoggerFactory.getLogger(V550UpgradePrerequisiteVerifier.class);

    private static final String ERR_MSG = "For an Artifactory HA installation, before upgrading to this version, " +
            "you first need to upgrade your cluster to Artifactory 5.4.6 to accommodate a database schema change " +
            "implemented in that version to support SHA256 checksums. You can consent downtime by appending " +
            "'artifactory.upgrade.allowAnyUpgrade.forVersion=" + ArtifactoryVersion.getCurrent()
            + "' in your artifactory.system.properties file.";

    @Override
    public boolean isInterested(ArtifactoryHome home, CompoundVersionDetails source, CompoundVersionDetails target) {
        //TODO [by dan]: perhaps it would be better to fail only if other nodes are up, if master is the only one
        //TODO [by dan]: up being on 5.4.6 does not matter (same case as pro)
        return home.isHaConfigured() && source.getVersion().before(ArtifactoryVersion.v546);
    }

    @Override
    public void convert(ArtifactoryHome artifactoryHome, CompoundVersionDetails source, CompoundVersionDetails target) {
        artifactoryHome.initArtifactorySystemProperties();
        String versionUpgradeProp = ConstantValues.allowAnyUpgrade.getString(artifactoryHome);
        if (isNotBlank(versionUpgradeProp)) {
            try {
                //User specified ok to upgrade with downtime?
                ArtifactoryVersion allowUpgrade = ArtifactoryVersion.fromVersionString(versionUpgradeProp);
                if (allowUpgrade != null && ArtifactoryVersion.v550m001.beforeOrEqual(allowUpgrade)) {
                    return;
                }
            } catch (Exception e) {
                String err = "Can't parse property upgrade.allowAnyUpgrade.forVersion " + versionUpgradeProp + ", aborting upgrade.";
                log.error(err);
                BootstrapLogger.error(err);
            }
        }
        //isInterested verified this is HA and coming from v < 5.4.6 so that's a no-no!
        log.error(ERR_MSG);
        BootstrapLogger.error(ERR_MSG);
        throw new IllegalStateException(ERR_MSG);
    }

    @Override
    public void assertConversionPreconditions(ArtifactoryHome home) throws ConverterPreconditionException {
        // nothing to do here, no files are being converted
    }
}

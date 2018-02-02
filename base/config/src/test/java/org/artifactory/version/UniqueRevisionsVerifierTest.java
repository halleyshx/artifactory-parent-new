package org.artifactory.version;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * This test is supposed to fail the build if more than one version holds the same revision - revisions must be unique
 * for all versions.
 *
 * @author Dan Feldman
 */
@Test
public class UniqueRevisionsVerifierTest {
    private static final Logger log = LoggerFactory.getLogger(UniqueRevisionsVerifierTest.class);

    public void verifyRevisions() {
        Assert.assertFalse(getUniqueRevisionCount() < ArtifactoryVersion.values().length,
                "Each Artifactory version must hold a unique build revision!");
    }

    private long getUniqueRevisionCount() {
        return Stream.of(ArtifactoryVersion.values())
                .map(ArtifactoryVersion::getRevision)
                .distinct()
                .count();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void verifyIsCurrentVersionNullVersionAndNullRev() {
        ArtifactoryVersion.isCurrentVersion(null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void verifyIsCurrentVersionNullVersion() {
        ArtifactoryVersion.isCurrentVersion(null, "whatever");
    }

    public void verifyIsNotCurrentVersion() {
        // Null revision
        Assert.assertFalse(ArtifactoryVersion.isCurrentVersion("whatever", null));
    }

    public void verifyIsCurrentVersion() {
        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("${forgot}", null));
        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("whatever-SNAPSHOT", null));
        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("whatever.x-DOWN-whatever", null));

        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("whatever", "${ff}"));
        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("whatever", "dev"));
        Assert.assertTrue(ArtifactoryVersion.isCurrentVersion("whatever", "dev-branch-456"));
    }

    public void verifyAllReleasedVersionMatches() {
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.name().equals("next")) {
                // nothing here
                continue;
            }
            String versionString = version.getValue();
            String[] split = versionString.split("\\.");
            Assert.assertTrue(split.length >= 3);
            if (split.length >= 4) {
                Assert.assertTrue(versionString.endsWith(".1"));
                if (version == ArtifactoryVersion.v130beta61) {
                    versionString = "1.3.0_m061";
                } else {
                    versionString = versionString.substring(0, versionString.length() - 2) + "_p001";
                }
            }
            versionString = versionString.
                    replace("u", "_p00").
                    replace("-beta-", "_m00").
                    replace("-rc-", "_m00").
                    replace("-rc", "_m00").
                    replace("-", "_");
            Matcher releaseVersionMatcher = ArtifactoryVersion.RELEASE_VERSION_PATTERN.matcher(versionString);
            if (releaseVersionMatcher.matches()) {
                if (version.after(ArtifactoryVersion.v542)) {
                    assertVersionMatch(version, versionString, releaseVersionMatcher);
                }
            } else {
                log.warn("Version " + version + " does not respect the regexp");
            }
        }
    }

    static void assertVersionMatch(ArtifactoryVersion artifactoryVersion, String releaseVersion,
            Matcher releaseVersionMatcher) {
        int major = Integer.valueOf(releaseVersionMatcher.group(1));
        int minor = Integer.valueOf(releaseVersionMatcher.group(2));
        int patch = Integer.valueOf(releaseVersionMatcher.group(3));
        long rev = major * 10_000_000L + minor * 100_000L + patch * 1000L;
        String preRelease = releaseVersionMatcher.group(4);
        if (StringUtils.isNotBlank(preRelease)) {
            char preChar = preRelease.substring(1, 2).charAt(0);
            Assert.assertTrue(preChar == 'm' || preChar == 'p',
                    "Pre release character '" + preChar + "' isn't a valid pre release character in " +
                            releaseVersion);
            int preReleaseNumber = Integer.valueOf(preRelease.substring(2));
            Assert.assertTrue(preReleaseNumber > 0,
                    "Version pattern pre release number is negative in " + releaseVersion);

            Assert.assertTrue(
                    artifactoryVersion.getValue()
                            .startsWith("" + major + "." + minor + "." + patch + "-" + preChar));
            if (preChar == 'm') {
                Assert.assertEquals(rev + preReleaseNumber, artifactoryVersion.getRevision());
            } else {
                Assert.assertEquals(rev + preReleaseNumber + 900L, artifactoryVersion.getRevision());
            }
        } else {
            // Full release no milestone or customer patch
            Assert.assertEquals(releaseVersion, artifactoryVersion.getValue());
            Assert.assertEquals(rev + 900L, artifactoryVersion.getRevision());
        }
    }

}

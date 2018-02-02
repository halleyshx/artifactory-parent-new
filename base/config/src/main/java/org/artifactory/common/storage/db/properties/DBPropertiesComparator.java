package org.artifactory.common.storage.db.properties;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.mojo.versions.ordering.MavenVersionComparator;

import java.util.Comparator;

/**
 * @author Gidi Shabat
 * (Originally for RTFACT-8297)
 */
public class DBPropertiesComparator implements Comparator<DbVersionInfo> {

    private final MavenVersionComparator versionComparator;

    public DBPropertiesComparator() {
        versionComparator = new MavenVersionComparator();
    }

    @Override
    public int compare(DbVersionInfo o1, DbVersionInfo o2) {
        String artifactoryVersion1 = o1.getArtifactoryVersion();
        String artifactoryVersion2 = o2.getArtifactoryVersion();
        DefaultArtifactVersion version1 = new DefaultArtifactVersion(artifactoryVersion1);
        DefaultArtifactVersion version2 = new DefaultArtifactVersion(artifactoryVersion2);
        return versionComparator.compare(version1, version2);
    }
}

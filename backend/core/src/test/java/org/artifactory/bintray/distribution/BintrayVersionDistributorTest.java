package org.artifactory.bintray.distribution;

import org.artifactory.api.bintray.distribution.reporting.DistributionReporter;
import org.artifactory.api.bintray.distribution.resolver.DistributionCoordinatesResolver;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.DistributionCoordinates;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.md.Properties;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Yuval Reches
 */
@Test
public class BintrayVersionDistributorTest extends ArtifactoryHomeBoundTest {

    private final String distRepo = "DistributionRepo";

    // RTFACT-12717
    public void debianCoordinates() {
        String debName = "damaged.deb";
        RepoPath debPath = new RepoPathImpl("debianTest", debName);
        String bintrayDebRepo = "deb";
        DistributionCoordinates originalCoordinates = new DistributionCoordinates(bintrayDebRepo, "pnp4::nagios", "0.6.19-1~debmon60+2", debName);
        DistributionRule rule = new DistributionRule("debRule", RepoType.Debian, null, null, originalCoordinates);
        Properties prop = new PropertiesImpl();

        DistributionCoordinatesResolver coordinates = new DistributionCoordinatesResolver(rule, debPath, prop, null);
        DistributionRepoDescriptor distRepoDescriptor = new DistributionRepoDescriptor();
        distRepoDescriptor.setKey(distRepo);
        BintrayVersionDistributor distributor = new BintrayVersionDistributor(null, null, null, distRepoDescriptor, null, null);

        RepoPath artifactDistPath = distributor.getArtifactDistPath(coordinates);
        assertEquals(artifactDistPath.getRepoKey(), distRepo);
        assertEquals(artifactDistPath.getPath(), bintrayDebRepo + "/" + debName);
    }

    // RTFACT-12717
    public void dockerCoordinates() {
        String manifestJson = "manifest.json";
        String bintrayRepo = "bintrayRepo";
        String dockerImageName = "ubuntu";
        String dockerTag = "latest";
        RepoPath manifestPath = new RepoPathImpl("dockerTest", manifestJson);
        DistributionRepoDescriptor distRepoDescriptor = new DistributionRepoDescriptor();
        distRepoDescriptor.setKey(distRepo);

        // For the copy, Artifactory takes the original docker values to create the final path in the Artifactory dist repo
        DistributionCoordinates originalCoordinates = new DistributionCoordinates(bintrayRepo, "ubuntu::123", "latest::latest", manifestJson);
        DistributionRule distRule = new DistributionRule("dockerRule", RepoType.Docker, null, null, originalCoordinates);

        PropertiesImpl manifestProps = new PropertiesImpl();
        manifestProps.put("docker.repoName", "ubuntu");
        manifestProps.put("docker.manifest", "latest");
        DistributionCoordinatesResolver resolver = new DistributionCoordinatesResolver(distRule, manifestPath, manifestProps, null);
        resolver.resolve(new DistributionReporter(false));
        BintrayVersionDistributor distributor = new BintrayVersionDistributor(null, null, null, distRepoDescriptor, null, null);

        RepoPath artifactDistPath = distributor.getDockerImageDistPath(resolver);
        assertEquals(artifactDistPath.getRepoKey(), distRepo);
        assertEquals(artifactDistPath.getPath(), bintrayRepo + "/" + dockerImageName + "/" + dockerTag);
    }
}
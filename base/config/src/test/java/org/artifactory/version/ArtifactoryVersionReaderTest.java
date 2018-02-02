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

package org.artifactory.version;

import ch.qos.logback.classic.Level;
import org.artifactory.test.TestUtils;
import org.jfrog.common.ResourceUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * Tests the ArtifactoryVersionReader.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryVersionReaderTest {

    public void readFromFileNoTimestamp() {
        File file = ResourceUtils.getResourceAsFile("/version/artifactory1.2.5.properties");
        CompoundVersionDetails version = ArtifactoryVersionReader.readFromFileAndFindVersion(file);
        Assert.assertNotNull(version, "Version should have been resolved");
        Assert.assertEquals(version.getVersion(), ArtifactoryVersion.v125, "Unexpected version");
        Assert.assertEquals(version.getTimestamp(), 0, "Unexpected timestamp");
    }

    public void readFromStreamNoTimestamp() {
        String resName = "/version/artifactory1.2.5.properties";
        InputStream propertiesFileStream = ResourceUtils.getResource(resName);
        CompoundVersionDetails version = ArtifactoryVersionReader.readAndFindVersion(propertiesFileStream, resName);
        Assert.assertNotNull(version, "Version should have been resolved");
        Assert.assertEquals(version.getVersion(), ArtifactoryVersion.v125, "Unexpected version");
    }

    public void readFromStreamWithTimestamp() {
        InputStream in = createInputStream("9.5", Integer.MAX_VALUE - 1, 789456);
        CompoundVersionDetails version = ArtifactoryVersionReader.readAndFindVersion(in, "readFromStreamWithTimestamp");
        Assert.assertNotNull(version, "Version should have been resolved");
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        ArtifactoryVersion latest =
                ArtifactoryVersion.getCurrent().getValue().equals("99.99.99") ? versions[versions.length - 2] : versions[versions.length - 1];
        Assert.assertEquals(version.getVersion(), latest, "Unexpected version");
        Assert.assertEquals(version.getTimestamp(), 789456, "Unexpected version");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failIfNullStream() {
        ArtifactoryVersionReader.readFromFileAndFindVersion((File) null);
    }

    public void readCurrentVersion() {
        ArtifactoryVersion current = ArtifactoryVersion.getCurrent();
        InputStream in = createInputStream(current.getValue(), current.getRevision());
        CompoundVersionDetails result = ArtifactoryVersionReader.readAndFindVersion(in, "readCurrentVersion");
        Assert.assertEquals(result.getVersion(), current);
    }

    public void readDevelopmentVersion() {
        InputStream in = createInputStream("${project.version}", 123);
        CompoundVersionDetails result = ArtifactoryVersionReader.readAndFindVersion(in, "readDevelopmentVersion");
        Assert.assertEquals(result.getVersion(), ArtifactoryVersion.getCurrent(),
                "Development version should always be the current version");
    }

    public void notExistingFutureVersion() {
        InputStream in = createInputStream("6.7.8.8", Integer.MAX_VALUE - 1);
        CompoundVersionDetails result = ArtifactoryVersionReader.readAndFindVersion(in, "notExistingFutureVersion");
        Assert.assertNotNull(result, "Got null result but expected the one before current version to be returned");
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        //release and master branches differ by the presence of ArtifactoryVersion.next
        ArtifactoryVersion latest =
                ArtifactoryVersion.getCurrent().getValue().equals("99.99.99") ? versions[versions.length - 2] : versions[versions.length - 1];
        Assert.assertEquals(result.getVersion(), latest,"Expected the latest released version to be returned");
    }

    private InputStream createInputStream(String version, long revision) {
        return new ByteArrayInputStream(String.format("artifactory.version=%s%n" +
                "artifactory.revision=%s%n", version, revision).getBytes());
    }

    private InputStream createInputStream(String version, int revision, long timestamp) {
        return new ByteArrayInputStream(String.format(
                "artifactory.version=%s%n" +
                        "artifactory.revision=%s%n" +
                        "artifactory.timestamp=%s%n", version, revision, timestamp).getBytes());
    }

    @BeforeClass
    public void setup() {
        TestUtils.setLoggingLevel(ArtifactoryVersionReader.class, Level.ERROR);
    }

    @AfterTest
    public void tearDown() {
        TestUtils.setLoggingLevel(ArtifactoryVersionReader.class, Level.INFO);
    }
}

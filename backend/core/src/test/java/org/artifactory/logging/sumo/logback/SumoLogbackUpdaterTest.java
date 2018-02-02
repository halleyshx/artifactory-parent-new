/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.sumo.logback;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.sumologic.SumoLogicConfigDescriptor;
import org.artifactory.logging.sumo.logback.SumoLogbackUpdater.UpdateData;
import org.jfrog.common.ResourceUtils;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * @author Shay Yaakov
 */
@Test
public class SumoLogbackUpdaterTest {

    private SumoLogbackUpdater sumoLogbackUpdater;
    private File baseTestDir;

    @BeforeMethod
    public void createTempDir() {
        skipOnWindows();
        baseTestDir = new File(System.getProperty("java.io.tmpdir"), "sumologictest");
        baseTestDir.mkdirs();
        assertTrue(baseTestDir.exists(), "Failed to create base test dir");
        sumoLogbackUpdater = new SumoLogbackUpdater();
    }

    private void skipOnWindows() {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw new SkipException("Skipping test on windows OS");
        }
    }

    @AfterMethod
    public void deleteTempDir() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(baseTestDir);
    }

    @Test
    public void testUpdateSumoAppenders() throws Exception {
        File logbackFile = new File(baseTestDir, "logback.xml");
        Files.copyFile(ResourceUtils.getResource("/org/artifactory/sumologic/logback.xml"), logbackFile);

        // --- Add sumo appenders for the first time ---
        SumoLogicConfigDescriptor sumoConfig = createSumoLogicConfigDescriptor();
        sumoConfig.setCollectorUrl("the-collector-url");
        sumoConfig.setEnabled(true);
        sumoLogbackUpdater.update(logbackFile, new UpdateData(sumoConfig, "art.host", "art.node"));

        File expectedFile = ResourceUtils.getResourceAsFile("/org/artifactory/sumologic/logback_enabled_no_proxy.xml");
        assertThat(FileUtils.readFileToString(logbackFile)).isEqualTo(FileUtils.readFileToString(expectedFile));

        // --- Update sumo appenders - disable and change collector url ---
        sumoConfig = createSumoLogicConfigDescriptor();
        sumoConfig.setCollectorUrl("the-other-collector-url");
        sumoConfig.setEnabled(false);
        sumoLogbackUpdater.update(logbackFile, new UpdateData(sumoConfig, "art.host", "art.node"));

        expectedFile = ResourceUtils.getResourceAsFile("/org/artifactory/sumologic/logback_disabled_no_proxy.xml");
        assertThat(FileUtils.readFileToString(logbackFile)).isEqualTo(FileUtils.readFileToString(expectedFile));

        // --- Update sumo appenders - enable, change collector url and add proxy ---
        sumoConfig = createSumoLogicConfigDescriptor();
        sumoConfig.setCollectorUrl("the-collector-url");
        sumoConfig.setEnabled(true);
        sumoConfig.setProxy(createProxy("proxy-host", 8888));
        sumoLogbackUpdater.update(logbackFile, new UpdateData(sumoConfig, "art.host", null));

        expectedFile = ResourceUtils.getResourceAsFile("/org/artifactory/sumologic/logback_enabled_with_proxy.xml");
        assertThat(FileUtils.readFileToString(logbackFile)).isEqualTo(FileUtils.readFileToString(expectedFile));

        // --- Update sumo appenders - remove proxy ---
        sumoConfig = createSumoLogicConfigDescriptor();
        sumoConfig.setCollectorUrl("the-collector-url");
        sumoConfig.setEnabled(true);
        sumoConfig.setProxy(null);
        sumoLogbackUpdater.update(logbackFile, new UpdateData(sumoConfig, "art.host", "art.node"));

        expectedFile = ResourceUtils.getResourceAsFile("/org/artifactory/sumologic/logback_enabled_no_proxy.xml");
        assertThat(FileUtils.readFileToString(logbackFile)).isEqualTo(FileUtils.readFileToString(expectedFile));
    }

    private ProxyDescriptor createProxy(String host, int port) {
        ProxyDescriptor proxy = new ProxyDescriptor();
        proxy.setHost(host);
        proxy.setPort(port);
        return proxy;
    }

    private SumoLogicConfigDescriptor createSumoLogicConfigDescriptor() {
        SumoLogicConfigDescriptor sumoConfig = new SumoLogicConfigDescriptor();
        sumoConfig.setClientId("the-client-id-" + System.currentTimeMillis());
        sumoConfig.setSecret("the-secret-" + System.currentTimeMillis());
        sumoConfig.setDashboardUrl("the-dashboard-url-" + System.currentTimeMillis());
        sumoConfig.setBaseUri("the-base-uri-" + System.currentTimeMillis());
        return sumoConfig;
    }
}
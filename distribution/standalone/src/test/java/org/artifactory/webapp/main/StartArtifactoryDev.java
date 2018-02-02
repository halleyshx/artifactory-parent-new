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

package org.artifactory.webapp.main;

import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.util.ListeningPortDetector;
import org.artifactory.webapp.WebappUtils;
import org.artifactory.webapp.main.AccessProcess.AccessProcessConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author yoavl
 */
@Deprecated
public class StartArtifactoryDev {

    public static final String MASTER_KEY = "0c1a1554553d487466687b339cd85f3d";
    static final String ACCESS_DEBUG_PORT_PROP = "jfrog.access.debug.port";
    static final String ACCESS_HOME_SYS_PROP = "jfrog.access.home";
    static final String ACCESS_DEFAULT_DEBUG_PORT = "6006";

    /**
     * Main function, starts the jetty server.
     */
    public static void main(String... args) throws IOException {
        System.setProperty(ConstantValues.dev.getPropertyName(), "true");
        File devArtHome = getArtifactoryDevHome();

        File devEtcDir = WebappUtils.populateAndGetEtcFolder(devArtHome);

        setSystemProperties(devArtHome);

        startAccessProcess();

        //Manually set the selector (needed explicitly here before any logger kicks in)
        // create the logger only after artifactory.home is set
        Server server = null;
        try {
            File jettyXml = new File(devEtcDir, "jetty.xml");
            if (!jettyXml.exists()) {
                throw new IllegalStateException("The Artifactory etc folder '" + devEtcDir.getAbsolutePath() +
                        "' should contain a jetty.xml and webdefault.xml files from " + devEtcDir.getAbsolutePath());
            }
            XmlConfiguration xmlConfiguration = new XmlConfiguration(jettyXml.toURL());

            server = new Server();
            xmlConfiguration.configure(server);

            server.start();
        } catch (Exception e) {
            System.err.println("Could not start the Jetty server: " + e);
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    System.err.println("Unable to stop the jetty server: " + e1);
                }
            }
        }
    }

    private static void setSystemProperties(File devArtHome) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        // set the logback.xml
        System.setProperty("logback.configurationFile", new File(devArtHome + "/etc/logback.xml").getAbsolutePath());

        // set default artifactory port
        if (System.getProperty(ListeningPortDetector.SYS_ARTIFACTORY_PORT) == null) {
            System.setProperty(ListeningPortDetector.SYS_ARTIFACTORY_PORT, "8080");
        }
    }

    private static File getArtifactoryDevHome() throws IOException {
        String homeProperty = System.getProperty("artifactory.home");
        File devArtHome;
        if (homeProperty != null) {
            devArtHome = new File(homeProperty).getAbsoluteFile();
        } else {
            devArtHome = new File(WebappUtils.getArtifactoryDevenv(), ".artifactory");
        }
        if (!devArtHome.exists() && !devArtHome.mkdirs()) {
            throw new RuntimeException("Failed to create home dir: " + devArtHome.getAbsolutePath());
        }
        System.setProperty(ArtifactoryHome.SYS_PROP, devArtHome.getAbsolutePath());
        return devArtHome;
    }

    static void startAccessProcess() throws IOException {
        startAccessProcess(0, false);
    }

    static void startAccessProcess(int port, boolean bundled) throws IOException {
        if (!Boolean.getBoolean("access.process.skip")) {
            // start Access server. re-use existing service is detected on the same port
            // the process will register a shutdown hook so we will let the JVM kill it
            File accessDevHome = getAccessDevHome();
            createMasterKeyFile(accessDevHome);
            AccessProcessConfig accessConfig = new AccessProcessConfig(accessDevHome).port(port).bundled(bundled);
            if (StringUtils.isNotBlank(System.getProperty(ACCESS_DEBUG_PORT_PROP))) {
                accessConfig = accessConfig.debug(Integer.valueOf(System.getProperty(ACCESS_DEBUG_PORT_PROP)), false);
            }
            new AccessProcess(accessConfig).start();
        }
    }

    private static void createMasterKeyFile(File accessDevHome) throws IOException {
        File etcDir = new File(accessDevHome, "etc");
        if (!etcDir.exists()) {
            etcDir.mkdir();
        }
        File keys = new File(etcDir, "keys");
        if (!keys.exists()) {
            keys.mkdir();
        }
        File masterKeyFile = new File(keys, "master.key");
        if (!masterKeyFile.exists()) {
            Files.write(masterKeyFile.toPath(), MASTER_KEY.getBytes());
        }
    }

    private static File getAccessDevHome() throws IOException {
        String homeProperty = System.getProperty(ACCESS_HOME_SYS_PROP);
        File devHome;
        if (homeProperty != null) {
            devHome = new File(homeProperty).getAbsoluteFile();
        } else {
            String artHomeProperty = System.getProperty(ArtifactoryHome.SYS_PROP);
            if (artHomeProperty != null) {
                devHome = new File(new File(artHomeProperty), "access"); //embedded home
            } else {
                devHome = new File(WebappUtils.getArtifactoryDevenv(), ".jfrog-access");
            }
        }
        if (!devHome.exists() && !devHome.mkdirs()) {
            throw new RuntimeException("Failed to create home dir: " + devHome.getAbsolutePath());
        }
        System.setProperty(ACCESS_HOME_SYS_PROP, devHome.getAbsolutePath());
        return devHome;
    }
}

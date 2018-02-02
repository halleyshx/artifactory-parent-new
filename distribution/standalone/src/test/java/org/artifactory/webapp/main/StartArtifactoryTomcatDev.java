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

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.apache.coyote.AbstractProtocol;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.webapp.WebappUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.artifactory.webapp.main.StartArtifactoryDev.startAccessProcess;

/**
 * Starts an Artifactory OSS instance with Tomcat embedded.
 *
 * @author yoavl
 */
public class StartArtifactoryTomcatDev {


    /**
     * Main function, starts the Tomcat server.
     */
    public static void main(String... args) throws IOException {
        File devArtHome = getArtifactoryDevHome(args);

        File devEtcDir = WebappUtils.getTestEtcFolder();
        WebappUtils.updateMimetypes(devEtcDir);
        WebappUtils.copyNewerDevResources(devEtcDir, devArtHome, true);

        setSystemProperties(devArtHome);

        startAccessProcess();

        Tomcat tomcat = startTomcat(devArtHome);
        tomcat.getServer().await();
    }

    static Tomcat startTomcat(File devArtHome) {
        return startTomcat(devArtHome, 8080);
    }

    static Tomcat startTomcat(File devArtHome, int port) {
        Tomcat tomcat = null;
        try {
            tomcat = new Tomcat();
            tomcat.setBaseDir(devArtHome + "/tomcat");
            // must serve root for the rewrite to process that.
            Path rootDir = Paths.get(WebappUtils.getWebappRoot(devArtHome, false).getAbsolutePath() ,"ROOT");
            Files.createDirectories(rootDir);

            tomcat.addContext(tomcat.getHost(),"", rootDir.toString());
            Context context = tomcat
                    .addWebapp("/artifactory", WebappUtils.getWebappRoot(devArtHome, false).getAbsolutePath());
            File tomcatResources = WebappUtils.getTomcatResources();
            ((StandardContext) context).setDefaultWebXml(new File(tomcatResources, "web.xml").getAbsolutePath());
            ((StandardContext) context)
                    .setDefaultContextXml(new File(tomcatResources, "context.xml").getAbsolutePath());
            context.setConfigFile(new File(tomcatResources, "artifactory.xml").toURI().toURL());
            tomcat.setPort(Integer.parseInt(System.getProperty("server.port", port + "")));
            ((AbstractProtocol) tomcat.getConnector().getProtocolHandler()).setSendReasonPhrase(false);
            // add Valves
            addRemoteIPValve(tomcat);
            addRewriteValve(tomcat);

            tomcat.start();
            return tomcat;
        } catch (Exception e) {
            System.err.println("Could not start the Tomcat server: " + e);
            if (tomcat != null) {
                try {
                    tomcat.stop();
                } catch (Exception e1) {
                    System.err.println("Unable to stop the Tomcat server: " + e1);
                }
            }
            throw new RuntimeException(e);
        }
    }

    static void addRemoteIPValve(Tomcat tomcat) {
        RemoteIpValve valve = new RemoteIpValve();
        valve.setProtocolHeader("X-Forwarded-Proto");

        tomcat.getEngine().getPipeline().addValve(valve);
    }

    static void addRewriteValve(Tomcat tomcat) throws Exception {
        String rules =
                        " RewriteRule ^/v2/(.*)$ /artifactory/v2/$1 [L]\n"+
                        "";
        RewriteValve valve = new RewriteValve();
        tomcat.getEngine().getPipeline().addValve(valve);

        valve.setConfiguration(rules);
    }

    static void setSystemProperties(File devArtHome) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty(ConstantValues.dev.getPropertyName(), "true");
        System.setProperty(ConstantValues.pluginScriptsRefreshIntervalSecs.getPropertyName(), "3");
        System.setProperty("logback.configurationFile", new File(devArtHome + "/etc/logback.xml").getAbsolutePath());
        System.setProperty("artifactory.addons.disabled", "aol");
        System.setProperty("staging.mode", "true"); // license checks against staging store
    }

    static File getArtifactoryDevHome(String[] args) throws IOException {
        String homeProperty = System.getProperty("artifactory.home");
        String prefix = args.length == 0 ? ".." : args[0];
        File devArtHome = new File(
                homeProperty != null ? homeProperty : prefix + "/devenv/.artifactory").getCanonicalFile();
        if (!devArtHome.exists() && !devArtHome.mkdirs()) {
            throw new RuntimeException("Failed to create home dir: " + devArtHome.getAbsolutePath());
        }
        System.setProperty(ArtifactoryHome.SYS_PROP, devArtHome.getAbsolutePath());
        return devArtHome;
    }
}

package org.artifactory.webapp;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.SystemUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.jfrog.client.util.PathUtils;
import org.jfrog.common.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.artifactory.webapp.main.StartArtifactoryDev.MASTER_KEY;

/**
 * @author Fred Simon on 9/12/16.
 */
public abstract class WebappUtils {
    private static final Logger log = LoggerFactory.getLogger(WebappUtils.class);

    private static File[] rootLocations;
    private static final String WEB_INF_LOCATION = "WEB-INF" + File.separator + "lib";
    private static final String INFO_WRITER_LOG =
            "<logger name=\"org.artifactory.info.InfoWriter\">\n" +
                    "        <level value=\"warn\"/>\n" +
                    "    </logger>";

    static {
        File currentFolder = new File(".").getAbsoluteFile();
        File userDir = SystemUtils.getUserDir().getAbsoluteFile();
        rootLocations = new File[]{
                currentFolder,
                currentFolder.getParentFile(),
                currentFolder.getParentFile().getParentFile(),
                currentFolder.getParentFile().getParentFile().getParentFile(),
                userDir,
                userDir.getParentFile(),
                // TODO: [by fsi] use good naming standards like ~/projects to look into
                new File(userDir, "projects")
        };
    }

    public static File getArtifactoryDevenv() throws IOException {
        String[] folderNames = new String[]{"devenv", "artifactory-devenv"};

        return find("Artifactory Devenv root", folderNames);
    }

    public static File getWebappRoot(File artHome, boolean integTest) throws IOException {
        // Find out if Pro or OSS version by using classpath contains Pro license manager
        boolean isPro = ResourceUtils.resourceExists("/org/artifactory/addon/LicenseProvider.class");
        boolean isJenkins = System.getenv("BUILD_NUMBER") != null;

        String toFindDescription;
        String[] folderNames;
        if (!integTest) {
            // Use only source code for UI dev
            toFindDescription = "Webapp Source Code root";
            folderNames = new String[]{
                    "artifactory-oss/web/war/src/main/webapp"
            };
        } else if (isPro) {
            toFindDescription = "Webapp target root";
            folderNames = new String[]{
                    "artifactory-pro/pro/war/target/artifactory",
                    "pro/war/target/artifactory",
                    "itest-pro/target/webapp",
                    "itest-online/target/webapp",
                    "webapp",
            };
        } else {
            toFindDescription = "Webapp Source OSS root";
            folderNames = new String[]{
                    "artifactory-oss/web/war/src/main/webapp",
                    "web/war/src/main/webapp",
                    "itest/target/webapp"
            };
        }
        File targetWebappRoot = find(toFindDescription, folderNames);
        // Copy the correct web.xml dev-env file only if not running in Jenkins (there the one in target is used)
        if (!isJenkins) {
            File webXmlFile;
            if (isPro) {
                webXmlFile = find("Pro web.xml file", new String[]{
                        "pro/war/src/main/webapp/WEB-INF/web.xml",
                        "artifactory-pro/pro/war/src/main/webapp/WEB-INF/web.xml"
                });
            } else {
                webXmlFile = find("OSS web.xml file", new String[]{
                        "web/war/src/main/webconf/WEB-INF/web.xml",
                        "artifactory-oss/web/war/src/main/webconf/WEB-INF/web.xml"
                });
            }
            FileUtils.copyFile(webXmlFile, new File(targetWebappRoot, "WEB-INF/web.xml"));
        }

        if (integTest) {
            if (artHome == null) {
                throw new IllegalArgumentException(
                        "Cannot create webapp folder in unit test without an artifactory home folder");
            }
            File webapp = new File(artHome, "webapp");
            IOFileFilter fileFilter = new WebappCopyFileFilter(targetWebappRoot, webapp);
            fileFilter = FileFilterUtils.makeSVNAware(fileFilter);
            FileUtils.copyDirectory(targetWebappRoot, webapp, fileFilter, true);
            return webapp;
        }
        return targetWebappRoot;
    }

    public static File getTestEtcFolder() throws IOException {
        String[] folderNames = new String[]{"artifactory-oss/distribution/standalone/src/test/etc",
                "distribution/standalone/src/test/etc"};

        return find("Test etc folder", folderNames);
    }

    public static File getTestHaResources() throws IOException {
        String[] folderNames = new String[]{"artifactory-pro/pro/war/src/test/resources/ha",
                "pro/war/src/test/resources/ha/"};

        return find("Test HA resources folder", folderNames);
    }

    public static File getAccessStandaloneJar() throws IOException {
        String[] folderNames = new String[] {
                "jfrog-access/server/application/target/access-application-3.1.x-SNAPSHOT-standalone.jar",
                "target/access-application-standalone.jar"
        };

        return find("Access Standalone Jar", folderNames);
    }

    public static File getTomcatResources() throws IOException {
        String[] folderNames = new String[]{"artifactory-oss/distribution/standalone/src/main/install/misc/tomcat"};
        return find("Tomcat resources folder", folderNames);
    }

    private static File find(String toFind, String[] folderNames) {
        for (String folderName : folderNames) {
            for (File rootLocation : rootLocations) {
                File file = new File(rootLocation, folderName);
                if (file.exists()) {
                    log.info("Found {} under {}", toFind, file.getAbsolutePath());
                    return file;
                }
            }
        }

        StringBuilder lookedInto = new StringBuilder("Trying to find " + toFind + " under:\n");
        for (File rootLocation : rootLocations) {
            for (String folderName : folderNames) {
                lookedInto.append(new File(rootLocation, folderName).getAbsolutePath()).append("\n");
            }
        }
        throw new RuntimeException(lookedInto.toString() + "FAILED!");
    }

    public static void updateMimetypes(File devEtcDir) {
        File defaultMimeTypes = ResourceUtils.getResourceAsFile("/META-INF/default/mimetypes.xml");
        File devMimeTypes = new File(devEtcDir, "mimetypes.xml");
        if (!devMimeTypes.exists() || defaultMimeTypes.lastModified() > devMimeTypes.lastModified()) {
            // override developer mimetypes file with newer default mimetypes file
            try {
                FileUtils.copyFile(defaultMimeTypes, devMimeTypes);
            } catch (IOException e) {
                System.err.println("Failed to copy default mime types file: " + e.getMessage());
            }
        }
    }

    /**
     * Copy newer files from the standalone dir to the working artifactory home dir
     *
     * @param isMaster -> secondary nodes get config from the database when they start
     */
    public static File copyNewerDevResources(File devEtcDir, File artHome, boolean isMaster) throws IOException {
        File etcDir = new File(artHome, "etc");
        IOFileFilter fileFilter = new EtcCopyFileFilter(devEtcDir, etcDir, isMaster);
        fileFilter = FileFilterUtils.makeSVNAware(fileFilter);
        FileUtils.copyDirectory(devEtcDir, etcDir, fileFilter, true);
        updateDefaultMimetypes(etcDir);
        deleteHaProps(etcDir);
        removeInfoWriterLogPrint(etcDir);

        /**
         * If the bootstrap already exists, it means it's not the first startup, so don't keep the original config file
         * or the etc folder will flood with bootstrap files
         */
        if (isMaster) {
            if (new File(etcDir, ArtifactoryHome.ARTIFACTORY_CONFIG_BOOTSTRAP_FILE).exists()) {
                new File(etcDir, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE).delete();
            }
        }
        copyMasterKey(etcDir);
        return etcDir;
    }

    private static void copyMasterKey(File etcDir) throws IOException {
        File securityDir = new File(etcDir, ArtifactoryHome.SECURITY_DIR_NAME);
        if (!securityDir.exists()) {
            securityDir.mkdir();
        }
        File masterKeyFile = new File(securityDir, "master.key");
        if (!masterKeyFile.exists()) {
            Files.write(masterKeyFile.toPath(), MASTER_KEY.getBytes());
        }
    }

    /**
     * The {@link org.artifactory.info.InfoWriter} output in devenv pisses me off
     */
    private static void removeInfoWriterLogPrint(File etcDir) throws IOException {
        File logbackFile = new File(etcDir, "logback.xml");
        if (logbackFile.exists()) {
            String logback = IOUtils.toString(new FileInputStream(logbackFile));
            if (!logback.contains("org.artifactory.info.InfoWriter")) {
                logback = logback.replace("</configuration>", INFO_WRITER_LOG);
            }
            try (FileWriter writer = new FileWriter(logbackFile, false)) {
                writer.write(logback);
            }
        }
    }

    private static void deleteHaProps(File homeEtcDir) throws IOException {
        if (!Boolean.parseBoolean(System.getProperty(ConstantValues.devHa.getPropertyName()))) {
            File haProps = new File(homeEtcDir, "artifactory.ha.properties");
            if (haProps.exists()) {
                FileUtils.forceDelete(haProps);
            }
        }
    }

    private static void updateDefaultMimetypes(File devEtcDir) {
        File defaultMimeTypes = ResourceUtils.getResourceAsFile("/META-INF/default/mimetypes.xml");
        File devMimeTypes = new File(devEtcDir, "mimetypes.xml");
        if (!devMimeTypes.exists() || defaultMimeTypes.lastModified() > devMimeTypes.lastModified()) {
            // override developer mimetypes file with newer default mimetypes file
            try {
                FileUtils.copyFile(defaultMimeTypes, devMimeTypes);
            } catch (IOException e) {
                System.err.println("Failed to copy default mime types file: " + e.getMessage());
            }
        }
    }

    public static File populateAndGetEtcFolder(File artHome) throws IOException {
        File etcDir = WebappUtils.copyNewerDevResources(getTestEtcFolder(), artHome, true);
        File jettyXml = new File(etcDir, "jetty.xml");
        if (jettyXml.exists()) {
            FileUtils.write(jettyXml,
                    FileUtils.readFileToString(jettyXml).
                            replace("@webapp.location@", WebappUtils.getWebappRoot(artHome, false).getAbsolutePath()).
                            replace("@artifactory.home@", artHome.getAbsolutePath())
                    //replace("@webdefault.location@", webdefaultXml.getAbsolutePath()).
            );
        }
        return etcDir;
    }

    private static class EtcCopyFileFilter extends AbstractFileFilter {
        private final File srcDir;
        private final File destDir;
        private final boolean isMasterNode;

        public EtcCopyFileFilter(File srcDir, File destDir, boolean isMasterNode) {
            this.srcDir = srcDir;
            this.destDir = destDir;
            this.isMasterNode = isMasterNode;
        }

        @Override
        public boolean accept(File srcFile) {
            if (srcFile.isDirectory()) {
                return true;    // don't exclude directories
            }
            // Jetty.xml always true
            String srcFileName = srcFile.getName();
            if ("jetty.xml".equals(srcFileName)) {
                return true;
            }
            String relativePath = PathUtils.getRelativePath(srcDir.getAbsolutePath(), srcFile.getAbsolutePath());
            File destFile = new File(destDir, relativePath);
            if (!destFile.exists() || srcFile.lastModified() > destFile.lastModified()) {
                if (isMasterNode) {
                    return true;
                } else if (srcFileName.equalsIgnoreCase("artifactory.config.xml")
                        || srcFileName.equalsIgnoreCase("artifactory.system.properties")
                        || srcFileName.equalsIgnoreCase("mimetypes.xml")) {
                    // These are config files that should be fetched from db by nodes
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    private static class WebappCopyFileFilter extends AbstractFileFilter {
        private final File srcDir;
        private final File destDir;

        public WebappCopyFileFilter(File srcDir, File destDir) {
            this.srcDir = srcDir;
            this.destDir = destDir;
        }

        @Override
        public boolean accept(File srcFile) {
            if (srcFile.isDirectory()) {
                if (srcFile.getAbsolutePath().endsWith(WEB_INF_LOCATION)) {
                    // WEB-INF lib no copy
                    return false;
                }
                if (srcFile.getAbsolutePath().endsWith("webapp")) {
                    // webapp UI not needed in integration tests
                    return false;
                }
                return true;    // don't exclude directories
            }
            if (srcFile.getName().endsWith(".jar")) {
                if (srcFile.getParentFile().getAbsolutePath().endsWith(WEB_INF_LOCATION)) {
                    // WEB-INF lib no copy
                    return false;
                }
            }
            String relativePath = PathUtils.getRelativePath(srcDir.getAbsolutePath(), srcFile.getAbsolutePath());
            File destFile = new File(destDir, relativePath);
            if (!destFile.exists() || srcFile.lastModified() > destFile.lastModified()) {
                return true;
            }
            return false;
        }
    }
}

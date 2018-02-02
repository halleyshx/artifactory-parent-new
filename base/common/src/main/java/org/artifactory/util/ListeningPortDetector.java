package org.artifactory.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * A helper class that attempts to detect the http port Artifactory is listening on.
 *
 * @author Yossi Shaul
 */
public class ListeningPortDetector {
    private static final Logger log = LoggerFactory.getLogger(ListeningPortDetector.class);

    public static final String SYS_ARTIFACTORY_PORT = "artifactory.port";

    /**
     * @return The detected http port or -1 if not found
     */
    public static int detect() {
        int port = detectFromSystemProperty();
        if (port == -1) {
            port = detectFromTomcatMBean();
        }
        return port;
    }

    private static int detectFromSystemProperty() {
        String portFromSystem = System.getProperty(SYS_ARTIFACTORY_PORT);
        if (portFromSystem != null) {
            try {
                return Integer.parseUnsignedInt(portFromSystem);
            } catch (NumberFormatException e) {
                log.warn("Unable to parse Artifactory port from system property: {}={}", SYS_ARTIFACTORY_PORT,
                        portFromSystem);
            }
        }
        return -1;
    }

    private static int detectFromTomcatMBean() {
        // the detector will return the port from the first HTTP/1.1 collector
        Set<TomcatUtils.ConnectorDetails> connectors = TomcatUtils.getHttpConnectors();
        if (!connectors.isEmpty()) {
            return connectors.iterator().next().getPort();
        }
        return -1;
    }
}

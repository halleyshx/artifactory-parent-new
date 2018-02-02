package org.artifactory.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class that attempts to detect the http port Artifactory is listening on.
 *
 * @author Yossi Shaul
 */
public class TomcatUtils {
    private static final Logger log = LoggerFactory.getLogger(org.artifactory.util.TomcatUtils.class);

    /**
     * @return Tomcat HTTP/1.1 connectors details or empty set is no such connector found.
     */
    public static Set<ConnectorDetails> getHttpConnectors() {
        try {
            // tomcat connectors detector - tomcat publishes the connectors under Tomcat/Connector/PORT(S)
            // the detector will fetch from the first HTTP/1.1 collector
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> connectors = mbs.queryNames(new ObjectName("*:type=Connector,*"),
                    Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
            return connectors.stream()
                    .map(c -> new ConnectorDetails(getSchemaAttribute(mbs, c), c.getKeyProperty("port")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to detect Tomcat connector: {}", e.getMessage());
            log.debug("Failed to detect Tomcat connector: {}", e);
            throw new RuntimeException(e);
        }
    }

    private static String getSchemaAttribute(MBeanServer mbs, ObjectName c) {
        try {
            return mbs.getAttribute(c, "scheme").toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse scheme attribute of connector: " + c);
        }
    }

    public static class ConnectorDetails {
        private String scheme;
        private int port;

        public ConnectorDetails(String scheme, String port) {
            this.scheme = StringUtils.isNotBlank(scheme) ? scheme : "http";
            this.port = Integer.parseInt(port);
        }

        /**
         * @return The scheme this connector is configured to use (http or https)
         */
        public String getScheme() {
            return scheme;
        }

        /**
         * @return The port this connector is configured to use
         */
        public int getPort() {
            return port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConnectorDetails that = (ConnectorDetails) o;
            return port == that.port && Objects.equals(scheme, that.scheme);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scheme, port);
        }

        @Override
        public String toString() {
            return "ConnectorDetails{scheme='" + scheme + '\'' + ", port=" + port + '}';
        }
    }
}
package org.artifactory.util;

import org.testng.annotations.Test;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit tests for the {@link TomcatUtils} class.
 *
 * @author Yossi Shaul
 */
@Test
public class TomcatUtilsTest {

    public void getConnectorsNoMBean() {
        Set<TomcatUtils.ConnectorDetails> connectors = TomcatUtils.getHttpConnectors();
        assertThat(connectors).isEmpty();
    }

    public void getWithSingleConnector() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("Tomcat:type=Connector,port=9091,scheme=http");
        try {
            mbs.registerMBean(new DummyTomcatConnector(), name);
            assertThat(TomcatUtils.getHttpConnectors()).hasSize(1)
                    .containsOnly(new TomcatUtils.ConnectorDetails("http", "9091"));
        } finally {
            unregister(mbs, name);
        }
    }

    public void getWithHttpsConnector() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("Tomcat:type=Connector,port=8743");
        try {
            mbs.registerMBean(new DummyTomcatConnector(), name);
            mbs.setAttribute(name, new Attribute("scheme", "https"));
            assertThat(TomcatUtils.getHttpConnectors()).hasSize(1)
                    .containsOnly(new TomcatUtils.ConnectorDetails("https", "8743"));
        } finally {
            unregister(mbs, name);
        }
    }

    public void getWithMultipleConnectors() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName https = new ObjectName("Tomcat:type=Connector,port=9043");
        ObjectName http = new ObjectName("Tomcat:type=Connector,port=9091");
        try {
            mbs.registerMBean(new TomcatUtilsTest.DummyTomcatConnector(), https);
            mbs.setAttribute(https, new Attribute("scheme", "https"));
            mbs.registerMBean(new TomcatUtilsTest.DummyTomcatConnector(), http);
            mbs.setAttribute(http, new Attribute("scheme", "http"));
            assertThat(TomcatUtils.getHttpConnectors()).hasSize(2)
                    .containsOnly(
                            new TomcatUtils.ConnectorDetails("http", "9091"),
                            new TomcatUtils.ConnectorDetails("https", "9043"));
        } finally {
            unregister(mbs, https);
            unregister(mbs, http);
        }
    }

    private void unregister(MBeanServer mbs, ObjectName https) throws Exception {
        if (mbs.isRegistered(https)) {
            mbs.unregisterMBean(https);
        }
    }

    public interface DummyTomcatConnectorMBean {
        String getprotocol();

        String getscheme();

        void setscheme(String scheme);
    }

    public static class DummyTomcatConnector implements DummyTomcatConnectorMBean {
        private String scheme = "http";

        @Override
        public String getprotocol() {
            return "HTTP/1.1";
        }

        @Override
        public String getscheme() {
            return scheme;
        }

        @Override
        public void setscheme(String scheme) {
            this.scheme = scheme;
        }
    }
}
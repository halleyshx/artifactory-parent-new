package org.artifactory.descriptor.sumologic;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.jfrog.common.DiffReference;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * Configuration for the Sumo Logic integration
 *
 * @author Shay Yaakov
 */
@XmlType(name = "SumoLogicConfigType", propOrder = {"enabled", "proxy", "clientId", "secret",
        "baseUri", "collectorUrl", "dashboardUrl"}, namespace = Descriptor.NS)
@GenerateDiffFunction
public class SumoLogicConfigDescriptor implements Descriptor {

    @XmlElement(defaultValue = "false")
    private boolean enabled;

    @XmlIDREF
    @XmlElement(name = "proxyRef")
    private ProxyDescriptor proxy;

    private String clientId;
    private String secret;
    private String baseUri;
    private String collectorUrl;
    private String dashboardUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @DiffReference
    public ProxyDescriptor getProxy() {
        return proxy;
    }

    public void setProxy(ProxyDescriptor proxy) {
        this.proxy = proxy;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getCollectorUrl() {
        return collectorUrl;
    }

    public void setCollectorUrl(String collectorUrl) {
        this.collectorUrl = collectorUrl;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SumoLogicConfigDescriptor that = (SumoLogicConfigDescriptor) o;

        if (enabled != that.enabled) return false;
        if (proxy != null ? !proxy.equals(that.proxy) : that.proxy != null) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (secret != null ? !secret.equals(that.secret) : that.secret != null) return false;
        if (baseUri != null ? !baseUri.equals(that.baseUri) : that.baseUri != null) return false;
        if (collectorUrl != null ? !collectorUrl.equals(that.collectorUrl) : that.collectorUrl != null) return false;
        return dashboardUrl != null ? dashboardUrl.equals(that.dashboardUrl) : that.dashboardUrl == null;

    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + (proxy != null ? proxy.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        result = 31 * result + (baseUri != null ? baseUri.hashCode() : 0);
        result = 31 * result + (collectorUrl != null ? collectorUrl.hashCode() : 0);
        result = 31 * result + (dashboardUrl != null ? dashboardUrl.hashCode() : 0);
        return result;
    }
}

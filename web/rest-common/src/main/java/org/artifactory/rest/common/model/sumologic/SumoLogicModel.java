package org.artifactory.rest.common.model.sumologic;

import org.artifactory.common.ConstantValues;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.rest.common.util.JsonUtil;

/**
 * @author Shay Yaakov
 */
public class SumoLogicModel extends BaseModel {

    private Boolean enabled;
    private String proxy;
    private String clientId;
    private String secret;
    private String email;
    private String redirectUrl;
    private String sumoBaseUrl;
    private String dashboardUrl;
    private int licenseType;

    public SumoLogicModel() {
        this.sumoBaseUrl = ConstantValues.sumoLogicApiUrl.getString();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getSumoBaseUrl() {
        return sumoBaseUrl;
    }

    public void setSumoBaseUrl(String sumoBaseUrl) {
        this.sumoBaseUrl = sumoBaseUrl;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }

    public int getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(int licenseType) {
        this.licenseType = licenseType;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

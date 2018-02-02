/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.model.general;

import org.artifactory.rest.common.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Basic footer implementation, must not include sensitive information and should be returned to the client when
 * anonymous access is disabled and the user is not logged in
 *
 * @author Shay Bagants
 */
public class BaseFooterImpl extends BaseModel implements Footer {

    private String serverName;
    private boolean userLogo;
    private String logoUrl;
    private boolean haConfigured;
    private boolean isAol;
    private String versionID;
    private boolean gaAccount;


    public BaseFooterImpl(String serverName, boolean userLogo, String logoUrl, boolean haConfigured, boolean isAol, String versionID, boolean gaAccount) {
        this.serverName = serverName;
        this.userLogo = userLogo;
        this.logoUrl = logoUrl;
        this.haConfigured = haConfigured;
        this.isAol = isAol;
        this.versionID = versionID;
        this.gaAccount = gaAccount;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public boolean isUserLogo() {
        return userLogo;
    }

    public void setUserLogo(boolean userLogo) {
        this.userLogo = userLogo;
    }

    @Override
    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isHaConfigured() {
        return haConfigured;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    @JsonProperty("isAol")
    public boolean isAol() {
        return isAol;
    }

    public String getVersionID() {
        return versionID;
    }

    public void setVersionID(String versionID) {
        this.versionID = versionID;
    }

    public boolean isGaAccount() {
        return gaAccount;
    }
}

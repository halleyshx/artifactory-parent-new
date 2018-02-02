/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.bintray;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.bintray.distribution.model.BintrayDistInfoModel;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Shay Yaakov
 */
public class BintrayDistUIModel extends BaseModel {

    private Boolean show;
    private String packageType;
    private String visibility;
    private String licenses;
    private String vcsUrl;
    private String errorMessage;

    public BintrayDistUIModel() {
    }

    public BintrayDistUIModel(BintrayDistInfoModel model) {
        this.packageType = model.packageType;
        this.visibility = model.visibility;
        this.licenses = model.licenses;
        this.vcsUrl = model.vcsUrl;
        this.errorMessage = model.errorMessage;
        this.show = StringUtils.isNotBlank(model.errorMessage);
    }

    public BintrayDistUIModel(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getShow() {
        return show;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getVcsUrl() {
        return vcsUrl;
    }

    public void setVcsUrl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

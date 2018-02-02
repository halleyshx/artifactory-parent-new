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

package org.artifactory.ui.rest.service.general;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.AddonsWebManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.message.SystemMessageDescriptor;
import org.artifactory.descriptor.repo.XrayDescriptor;
import org.artifactory.descriptor.security.sso.SamlSettings;
import org.artifactory.descriptor.trashcan.TrashcanConfigDescriptor;
import org.artifactory.rest.common.model.xray.XrayConfigModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.general.BaseFooterImpl;
import org.artifactory.ui.rest.model.general.Footer;
import org.artifactory.ui.rest.model.general.FooterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetFooterService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        Footer footer;
        String versionInfo = getVersionInfo();
        String versionID = getVersionID(versionInfo);

        //even when the anon access disabled, there are exceptions where we allow anon access login.
        if (authorizationService.isAnonymous() && !authorizationService.isAnonAccessEnabled()) {
            footer = new BaseFooterImpl(getServer(), isUserLogo(), getLogoUrl(), isHaConfigured(), isAol(), versionID, isProductionEnv());
        } else {
            footer = new FooterImpl(getFooterLicenseInfo(), versionInfo, getCopyrights(), getCopyRightsUrl(),
                    getBuildNum(), isAol(), isDedicatedAol(), isHttpSsoEnabledAOL(), versionID, isUserLogo(),
                    getLogoUrl(), getServer(), getSystemMessage(), isHelpLinksEnabled(), getCurrentServerId(),
                    isTrashDisabled(), allowPermDeletes(), getSamlAutoRedirect(), isXRayConfigured(), isXRayEnabled(),
                    isXRayLicenseInstalled(), isHaConfigured(), isProductionEnv());
        }
        response.iModel(footer);
    }

    /**
     * get version id (OSS / PRO / ENT)
     *
     * @param versionInfo - edition version info
     */
    private String getVersionID(String versionInfo) {
        String versionID = "OSS";
        switch (versionInfo) {
            case "Artifactory Enterprise":
                versionID = "ENT";
                break;
            case "Artifactory Professional":
                versionID = "PRO";
                break;
            case "Artifactory Online":
                versionID = "PRO";
                break;
            default:
                versionID = "OSS";
                break;
        }
        return versionID;
    }

    private String getCurrentServerId() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        return haCommonAddon.getCurrentMemberServerId();
    }

    /**
     * return version info
     *
     * @return version info text
     */
    private String getVersionInfo() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        if (addonsManager instanceof OssAddonsManager){
            return "Artifactory OSS";
        }
        if (isAol()) {
            return "Artifactory Online";
        } else if (addonsManager.isHaLicensed()) {
            return "Artifactory Enterprise";
        } else if (addonsManager.getProAndAolLicenseDetails().getType().equals("Commercial")) {
            return "Artifactory Professional";
        }  else if (addonsManager.getProAndAolLicenseDetails().getType().equals("Trial")) {
            return "Artifactory OSS";
        } else {
            // No license and we know that the instance is PRO instance
            return "Artifactory Professional";
        }
    }

    private boolean isAol() {
        return ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class).isAol();
    }

    private boolean isDedicatedAol() {
        return this.isAol() && ArtifactoryHome.get().getArtifactoryProperties().getBooleanProperty(ConstantValues.aolDedicatedServer);
    }

    private boolean isHttpSsoEnabledAOL() {
        return ConstantValues.aolSecurityHttpSsoEnabled.getBoolean();
    }

    private boolean isTrashDisabled() {
        TrashcanConfigDescriptor trashcanConfig = centralConfigService.getDescriptor().getTrashcanConfig();
        return !trashcanConfig.isEnabled();
    }

    private boolean allowPermDeletes() {
        TrashcanConfigDescriptor trashcanConfig = centralConfigService.getDescriptor().getTrashcanConfig();
        return trashcanConfig.isAllowPermDeletes();
    }

    /**
     * return version info
     *
     * @return version info text
     */
    private String getBuildNum() {
        CoreAddons addon =  ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class);
        return addon.getBuildNum();
    }


    /**
     * return footer license message
     *
     * @return footer text message
     */
    private String getFooterLicenseInfo() {
        AddonsWebManager addonsManager = ContextHelper.get().beanForType(AddonsWebManager.class);
        return addonsManager.getFooterMessage(authorizationService.isAdmin());
    }

    /**
     * get copyrights data
     *
     * @return copy rights data
     */
    private String getCopyrights() {
        LocalDate localDate = LocalDate.now();
        String copyRights = "© Copyright " + localDate.getYear() + " JFrog Ltd";
        return copyRights;
    }

    /**
     * get copyrights url
     *
     * @return copyrights url
     */
    private String getCopyRightsUrl() {
        return "http://www.jfrog.com";
    }

    /**
     * check if user logo exist
     *
     * @return true if user logo exist
     */
    private boolean isUserLogo() {
        String logoDir = ContextHelper.get().getArtifactoryHome().getLogoDir().getAbsolutePath();
        File sourceFile = new File(logoDir, "logo");
        boolean fileExist = sourceFile.canRead();
        if (fileExist) {
            return true;
        }
        return false;
    }

    /**
     * return logo url link
     *
     * @return
     */
    private String getLogoUrl() {
        return centralConfigService.getDescriptor().getLogo();
    }

    /**
     * return logo url link
     *
     * @return
     */
    private String getServer() {
        return centralConfigService.getDescriptor().getServerName();
    }

    /**
     *  System message descriptor.
     */
    private SystemMessageDescriptor getSystemMessage() {
        return Optional.ofNullable(centralConfigService.getDescriptor().getSystemMessageConfig())
                .orElse(new SystemMessageDescriptor());
    }

    public boolean isHelpLinksEnabled() {
        return centralConfigService.getDescriptor().isHelpLinksEnabled();
    }

    /**
     * Return the SAML redirect configuration
     *
     * @return SAML redirect configuration
     */
    private boolean getSamlAutoRedirect() {
        SamlSettings samlSettings = centralConfigService.getDescriptor().getSecurity().getSamlSettings();
        return samlSettings != null && samlSettings.isAutoRedirect();
    }

    /**
     * Check if XRay is configured
     *
     * @return true if XRay is configured, false otherwise
     */
    private boolean isXRayConfigured() {
        XrayConfigModel xrayConfigModel = new XrayConfigModel(centralConfigService.getDescriptor().getXrayConfig());
        return xrayConfigModel.getXrayBaseUrl() != null;
    }

    /**
     * Check if XRay enabled
     *
     * @return true if XRay is enabled
     */
    private boolean isXRayEnabled() {
        XrayDescriptor xrayConfig = centralConfigService.getDescriptor().getXrayConfig();
        return xrayConfig != null && xrayConfig.isEnabled();
    }

    /**
     * Check if an XRay license installed
     *
     * @return true if XRay license is installed
     */
    private boolean isXRayLicenseInstalled() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return !addonsManager.getLicenseTypeForProduct(AddonsManager.XRAY_PRODUCT_NAME).equals("OSS");
    }

    /**
     * Check if the env is HA env (configured, not enabled)
     * @return true if HA is configured
     */
    private boolean isHaConfigured() {
        return ArtifactoryHome.get().isHaConfigured();
    }

    /**
     *  Check if Google Analytics should track into staging or production profile
     * @return true if environment variable 'GOOGLE_ANALYTICS_ACCOUNT' equal to 'staging'
     */
    private boolean isProductionEnv() {
        String googleAnalyticsAccount = System.getenv("GOOGLE_ANALYTICS_ACCOUNT");
        return googleAnalyticsAccount == null || StringUtils.equals(googleAnalyticsAccount, "production");
    }
}

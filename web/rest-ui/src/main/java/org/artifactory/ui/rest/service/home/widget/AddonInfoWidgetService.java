package org.artifactory.ui.rest.service.home.widget;

import org.artifactory.addon.*;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.XrayDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.home.AddonModel;
import org.artifactory.ui.rest.model.home.HomeWidgetModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddonInfoWidgetService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<AddonInfo> installedAddons = addonsManager.getInstalledAddons(null);
        HashMap<String, AddonInfo> addonInfoMap = new HashMap<>();
        installedAddons.forEach(addonInfo -> addonInfoMap.put(addonInfo.getAddonName(), addonInfo));
        List<AddonModel> addonModels = new ArrayList<>();
        updateAddonList(addonInfoMap, addonModels);

        HomeWidgetModel model = new HomeWidgetModel("Addons");
        model.addData("addons", addonModels);
        response.iModel(model);
    }

    /**
     * update addon list data
     *
     * @param addonInfoMap - addon info map
     * @param addonModels  - addons models
     */
    private void updateAddonList(HashMap<String, AddonInfo> addonInfoMap, List<AddonModel> addonModels) {
        if (!isAol()) {
            addonModels.add(new AddonModel(AddonType.HA, addonInfoMap.get("ha"), getAddonLearnMoreUrl("ha"),
                    getAddonConfigureUrl(AddonType.HA.getConfigureUrlSuffix())));
        }
        addonModels.add(new AddonModel(AddonType.DISTRIBUTION, getAddonInfo(AddonType.DISTRIBUTION, AddonState.ACTIVATED), getAddonLearnMoreUrl("distributionrepo"), getAddonConfigureUrl(AddonType.DISTRIBUTION.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.XRAY, getXrayAddonInfo(), "https://www.jfrog.com/xray/", "https://www.jfrog.com/confluence/display/XRAY/Welcome+to+JFrog+Xray"));
        addonModels.add(new AddonModel(AddonType.JFROG_CLI, getAddonInfo(AddonType.JFROG_CLI, AddonState.ACTIVATED), "https://www.jfrog.com/article/jfrog-cli-automation-scripts/", "https://www.jfrog.com/confluence/display/CLI/Welcome+to+JFrog+CLI"));
        addonModels.add(new AddonModel(AddonType.BUILD, addonInfoMap.get("build"), getAddonLearnMoreUrl("build"), getAddonConfigureUrl(AddonType.BUILD.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.DOCKER, addonInfoMap.get("docker"), getAddonLearnMoreUrl("docker"), getAddonConfigureUrl(AddonType.DOCKER.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.REPLICATION, addonInfoMap.get("replication"), getAddonLearnMoreUrl("replication"), getAddonConfigureUrl(AddonType.REPLICATION.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.MULTIPUSH, getAddonInfo(AddonType.MULTIPUSH), String.format(ConstantValues.addonsInfoUrl.getString(), "replication"), getAddonConfigureUrl(AddonType.MULTIPUSH.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.AQL, getAqlAddonInfo(), getAddonLearnMoreUrl("aql"), getAddonConfigureUrl(AddonType.AQL.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.S3, getAddonInfo(AddonType.S3), getAddonLearnMoreUrl("filestore"), getAddonConfigureUrl(AddonType.S3.getConfigureUrlSuffix()))); //todo change more info link to s3-filestore
        //addonModels.add(new AddonModel(AddonType.HDFS, getAddonInfo(AddonType.HDFS), getAddonLearnMoreUrl("addon-hdfs"), getAddonConfigureUrl(AddonType.HDFS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.GCS, getAddonInfo(AddonType.GCS), getAddonLearnMoreUrl("gcs"), getAddonConfigureUrl(AddonType.GCS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SHARDING, getAddonInfo(AddonType.SHARDING), getAddonLearnMoreUrl("sharding"), getAddonConfigureUrl(AddonType.SHARDING.getConfigureUrlSuffix())));
        AddonInfo aolAddonPlugin;
        aolAddonPlugin = getUserPluginAddonInfo(addonInfoMap);
        addonModels.add(new AddonModel(AddonType.PLUGINS, aolAddonPlugin, getAddonLearnMoreUrl("plugins"), getAddonConfigureUrl(AddonType.PLUGINS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SMART_REPO, getAddonInfoForPro(AddonType.SMART_REPO), getAddonLearnMoreUrl("smart-remote-repositories"), getAddonConfigureUrl(AddonType.SMART_REPO.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SUMOLOGIC, getAddonInfo(AddonType.SUMOLOGIC, AddonState.ACTIVATED), getAddonLearnMoreUrl("loganalytics"), getAddonConfigureUrl(AddonType.SUMOLOGIC.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.NUGET, addonInfoMap.get("nuget"), getAddonLearnMoreUrl("nuget"), getAddonConfigureUrl(AddonType.NUGET.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.NPM, addonInfoMap.get("npm"), getAddonLearnMoreUrl("npm"), getAddonConfigureUrl(AddonType.NPM.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.COMPOSER, addonInfoMap.get("composer"), getAddonLearnMoreUrl("composer"), getAddonConfigureUrl(AddonType.COMPOSER.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.BOWER, addonInfoMap.get("bower"), getAddonLearnMoreUrl("bower"), getAddonConfigureUrl(AddonType.BOWER.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.COCOAPODS, addonInfoMap.get("cocoapods"), getAddonLearnMoreUrl("cocoapods"), getAddonConfigureUrl(AddonType.COCOAPODS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.CONAN, addonInfoMap.get("conan"), getAddonLearnMoreUrl("conan"), getAddonConfigureUrl(AddonType.CONAN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.REST, addonInfoMap.get("rest"), getAddonLearnMoreUrl("rest"), getAddonConfigureUrl(AddonType.REST.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.GITLFS, addonInfoMap.get("git-lfs"), getAddonLearnMoreUrl("git-lfs"), getAddonConfigureUrl(AddonType.GITLFS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.VAGRANT, addonInfoMap.get("vagrant"), getAddonLearnMoreUrl("vagrant"), getAddonConfigureUrl(AddonType.VAGRANT.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.LDAP, addonInfoMap.get("ldap"), getAddonLearnMoreUrl("ldap"), getAddonConfigureUrl(AddonType.LDAP.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SSO, addonInfoMap.get("sso"), getAddonLearnMoreUrl("sso"), getAddonConfigureUrl(AddonType.SSO.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.OAUTH, getAddonInfoForPro(AddonType.OAUTH), getAddonLearnMoreUrl("oauth-integration"), getAddonConfigureUrl(AddonType.OAUTH.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SSH, getAddonInfoForPro(AddonType.SSH), getAddonLearnMoreUrl("ssh"), getAddonConfigureUrl(AddonType.SSH.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.VCS, addonInfoMap.get("vcs"), getAddonLearnMoreUrl("vcs"), getAddonConfigureUrl(AddonType.VCS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.YUM, addonInfoMap.get("rpm"), getAddonLearnMoreUrl("rpm"), getAddonConfigureUrl(AddonType.YUM.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.DEBIAN, addonInfoMap.get("debian"), getAddonLearnMoreUrl("debian"), getAddonConfigureUrl(AddonType.DEBIAN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.OPKG, addonInfoMap.get("opkg"), getAddonLearnMoreUrl("opkg"), getAddonConfigureUrl(AddonType.OPKG.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.GEMS, addonInfoMap.get("gems"), getAddonLearnMoreUrl("gems"), getAddonConfigureUrl(AddonType.GEMS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.PYPI, addonInfoMap.get("pypi"), getAddonLearnMoreUrl("pypi"), getAddonConfigureUrl(AddonType.PYPI.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.CHEF, addonInfoMap.get("chef"), getAddonLearnMoreUrl("chef"), getAddonConfigureUrl(AddonType.CHEF.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.PUPPET, addonInfoMap.get("puppet"), getAddonLearnMoreUrl("puppet"), getAddonConfigureUrl(AddonType.PUPPET.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.HELM, addonInfoMap.get("helm"), getAddonLearnMoreUrl("helm"), getAddonConfigureUrl(AddonType.HELM.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.PROPERTIES, addonInfoMap.get("properties"), getAddonLearnMoreUrl("properties"), getAddonConfigureUrl(AddonType.PROPERTIES.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SEARCH, addonInfoMap.get("search"), getAddonLearnMoreUrl("search"), getAddonConfigureUrl(AddonType.SEARCH.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.LAYOUTS, addonInfoMap.get("layouts"), getAddonLearnMoreUrl("layouts"), getAddonConfigureUrl(AddonType.LAYOUTS.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.LICENSES, addonInfoMap.get("license"), getAddonLearnMoreUrl("license"), getAddonConfigureUrl(AddonType.LICENSES.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.MAVEN_PLUGIN, getAddonInfo(AddonType.MAVEN_PLUGIN, AddonState.ACTIVATED), getAddonLearnMoreUrl("maven"), getAddonConfigureUrl(AddonType.MAVEN_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.GRADLE_PLUGIN, getAddonInfo(AddonType.GRADLE_PLUGIN, AddonState.ACTIVATED), getAddonLearnMoreUrl("gradle"), getAddonConfigureUrl(AddonType.GRADLE_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.JENKINS_PLUGIN, getAddonInfo(AddonType.JENKINS_PLUGIN, AddonState.ACTIVATED), getAddonLearnMoreUrl("build"), getAddonConfigureUrl(AddonType.JENKINS_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.BAMBOO_PLUGIN, getAddonInfo(AddonType.BAMBOO_PLUGIN, AddonState.ACTIVATED ), getAddonLearnMoreUrl("build"), getAddonConfigureUrl(AddonType.BAMBOO_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.TC_PLUGIN, getAddonInfo(AddonType.TC_PLUGIN, AddonState.ACTIVATED), getAddonLearnMoreUrl("build"), getAddonConfigureUrl(AddonType.TC_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.MSBUILD_PLUGIN, getAddonInfo(AddonType.MSBUILD_PLUGIN, AddonState.ACTIVATED), getAddonLearnMoreUrl("tfs-integration"), getAddonConfigureUrl(AddonType.MSBUILD_PLUGIN.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.SBT, getAddonInfo(AddonType.SBT, AddonState.ACTIVATED), getAddonLearnMoreUrl("sbt"), getAddonConfigureUrl(AddonType.SBT.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.IVY, getAddonInfo(AddonType.IVY, AddonState.ACTIVATED), getAddonLearnMoreUrl("ivy"), getAddonConfigureUrl(AddonType.IVY.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.FILTERED_RESOURCES, addonInfoMap.get("filtered-resources"), getAddonLearnMoreUrl("filtered-resources"), getAddonConfigureUrl(AddonType.FILTERED_RESOURCES.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.P2, addonInfoMap.get("p2"), getAddonLearnMoreUrl("p2"), getAddonConfigureUrl(AddonType.P2.getConfigureUrlSuffix())));
        addonModels.add(new AddonModel(AddonType.WEBSTART, addonInfoMap.get("webstart"), getAddonLearnMoreUrl("webstart"), getAddonConfigureUrl(AddonType.WEBSTART.getConfigureUrlSuffix())));
    }

    /**
     * get User plugin for aol or pro
     *
     * @param addonInfoMap - addon info map
     */
    private AddonInfo getUserPluginAddonInfo(HashMap<String, AddonInfo> addonInfoMap) {
        AddonInfo aolAddonPlugin;
        if (isAol()) {
            aolAddonPlugin = getAddonInfo(AddonType.PLUGINS, AddonState.INACTIVATED);
        } else {
            aolAddonPlugin = addonInfoMap.get("plugins");
        }
        return aolAddonPlugin;
    }

    /**
     * return add on lean more url
     *
     * @param addonId - addon id
     */
    private String getAddonLearnMoreUrl(String addonId) {
        return String.format(ConstantValues.addonsInfoUrl.getString(), addonId);
    }

    /**
     * return add on configure more url
     *
     * @param addonId - addon id
     */
    private String getAddonConfigureUrl(String addonId) {
        return String.format(ConstantValues.addonsConfigureUrl.getString(), addonId);
    }

    private AddonInfo getAqlAddonInfo() {
        AddonInfo addonInfo = new AddonInfo(AddonType.AQL.getAddonName(),
                AddonType.AQL.getAddonDisplayName(), "", AddonState.ACTIVATED, new Properties(), 10);
        return addonInfo;
    }

    private AddonInfo getXrayAddonInfo() {
        AddonState addonState;
        if (ContextHelper.get().beanForType(AddonsManager.class).isXrayLicensed()) {
            XrayDescriptor xrayDescriptor = centralConfigService.getDescriptor().getXrayConfig();
            if (xrayDescriptor == null) {
                addonState = AddonState.NOT_CONFIGURED;
            } else if (xrayDescriptor.isEnabled()) {
                addonState = AddonState.ACTIVATED;
            } else {
                addonState = AddonState.DISABLED;
            }
        } else {
            addonState = AddonState.NOT_LICENSED;
        }
        return new AddonInfo(AddonType.XRAY.getAddonName(), AddonType.XRAY.getAddonDisplayName(), "", addonState,
                new Properties(), 10);
    }

    /**
     * get addon info
     *
     * @param type - addon type
     */
    private AddonInfo getAddonInfo(AddonType type) {
        boolean haLicensed = ContextHelper.get().beanForType(AddonsManager.class).isHaLicensed();
        return new AddonInfo(type.getAddonName(), type.getAddonDisplayName(), "",
                (haLicensed) ? AddonState.ACTIVATED : AddonState.NOT_LICENSED, new Properties(), 10);
    }

    /**
     * get addon info
     *
     * @param type - addon type
     */
    private AddonInfo getAddonInfoForPro(AddonType type) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return new AddonInfo(type.getAddonName(), type.getAddonDisplayName(), "",
                (addonsManager.isLicenseInstalled()) ? AddonState.ACTIVATED : AddonState.NOT_LICENSED,
                new Properties(), 10);
    }
    /**
     * get addon info
     *
     * @param type - addon type
     */
    private AddonInfo getAddonInfo(AddonType type, AddonState state) {
        return new AddonInfo(type.getAddonName(), type.getAddonDisplayName(), "", state, new Properties(), 10);
    }

    /**
     * if true - aol license
     */
    private boolean isAol() {
        return ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class).isAol();
    }
}

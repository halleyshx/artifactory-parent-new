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

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.actions;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.bower.BowerAddon;
import org.artifactory.addon.chef.ChefAddon;
import org.artifactory.addon.composer.ComposerAddon;
import org.artifactory.addon.puppet.PuppetAddon;
import org.artifactory.addon.pypi.PypiAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.NodeRepoTypeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds tabs list to be added to a tree node
 *
 * @author Shay Yaakov
 */
@Component
public class TreeNodeTabsPopulator {

    @Autowired
    private RepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private AddonsManager addonsManager;

    private NodeRepoTypeHelper nodeRepoTypeHelper;

    @PostConstruct
    private void init() {
        nodeRepoTypeHelper = new NodeRepoTypeHelper(addonsManager, repoService);
    }

    public void populateForRepository(TabsAndActions tabsAndActions) {
        List<TabOrAction> tabs = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        if ("remote".equals(repoType) || "virtual".equals(repoType) || "trash".equals(repoType)) {
            tabs.add(general());
        } else {
            tabs.add(general());
            addCommonsTabs(tabs, repoPath);
        }
        tabsAndActions.setTabs(tabs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public void populateForFolder(TabsAndActions tabsAndActions) {
        List<TabOrAction> tabs = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        boolean isCached = tabsAndActions.getCached();
        if ("remote".equals(repoType) || "trash".equals(repoType)) {
            tabs.add(general());
        } else if ("distribution".equals(repoType)) {
            tabs.add(general());
            addCommonsTabs(tabs, repoPath);
        }
        else if ("virtual".equals(repoType)){
            tabs.add(general());
            if (!isCached) {
                tabsAndActions.setTabs(tabs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                return;
            }
            repoPath = repoService.getVirtualFolderInfo(repoPath).getRepoPath();
            RepoPath finalRepoPath = repoPath;
            if(repoService.getLocalAndCachedRepoDescriptors().stream()
                    .anyMatch(e -> e.getKey().equals(finalRepoPath.getRepoKey()))) {
                tabs.addAll(dockerV1(repoPath));
                tabs.add(dockerV2(repoPath));
                addCommonsTabs(tabs, repoPath);
            }
        } else {
            tabs.add(general());
            tabs.addAll(dockerV1(repoPath));
            tabs.add(dockerV2(repoPath));
            tabs.add(conan(repoPath));
            addCommonsTabs(tabs, repoPath);
        }
        tabsAndActions.setTabs(tabs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public void populateForFile(TabsAndActions tabsAndActions) {
        List<TabOrAction> tabs = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        switch (repoType){
            case "remote" : tabs.add(general());break;
            case "trash" :  tabs.add(general());break;
            case "virtual" : fillVirtualTabs(tabs,tabsAndActions,repoPath);break;
            case "distribution" : fillDistributionTabs(tabs,repoPath);break;
            default: fillDefaultTabs(tabs,repoPath);
        }
        tabsAndActions.setTabs(tabs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private void fillDefaultTabs(List<TabOrAction> tabs, RepoPath repoPath) {
        tabs.add(general());
        addTabsForCachedOrLocalFile(tabs, repoPath);
    }

    private void fillDistributionTabs(List<TabOrAction> tabs, RepoPath repoPath) {
        tabs.add(general());
        addCommonsTabs(tabs,repoPath);
    }

    private void fillVirtualTabs(List<TabOrAction> tabs, TabsAndActions tabsAndActions,
            RepoPath repoPath) {
        tabs.add(general());
        if ( ! tabsAndActions.getCached()) {
            return;
        }
        repoPath = repoService.getVirtualFileInfo(repoPath).getRepoPath();
        RepoPath finalRepoPath = repoPath;
        if(repoService.getLocalAndCachedRepoDescriptors().stream()
                .anyMatch(e -> e.getKey().equals(finalRepoPath.getRepoKey()))) {
            addTabsForCachedOrLocalFile(tabs, repoPath);
        }
    }

    private void addTabsForCachedOrLocalFile(List<TabOrAction> tabs, RepoPath repoPath) {
        addSpesificFileTabs(tabs, repoPath);
        addCommonsTabs(tabs, repoPath);
        tabs.add(builds());
    }

    private void addCommonsTabs(List<TabOrAction> tabs, RepoPath repoPath) {
        tabs.add(effectivePermissions(repoPath));
        tabs.add(properties());
        tabs.add(watch(repoPath));
    }

    private void addSpesificFileTabs(List<TabOrAction> tabs, RepoPath repoPath) {
        tabs.add(pom(repoPath));
        tabs.add(xml(repoPath));
        tabs.add(rpm(repoPath));
        tabs.add(nuget(repoPath));
        tabs.add(gem(repoPath));
        tabs.add(npm(repoPath));
        tabs.add(debian(repoPath));
        tabs.add(opkg(repoPath));
        tabs.add(bower(repoPath));
        tabs.add(composer(repoPath));
        tabs.add(chef(repoPath));
        tabs.add(pypi(repoPath));
        tabs.add(puppet(repoPath));
        tabs.add(helm(repoPath));
    }

    private TabOrAction builds() {
        if (!authService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            return new TabOrAction("Builds");
        }
        return null;
    }

    private TabOrAction pom(RepoPath repoPath) {
        if (MavenNaming.isPom((repoPath.getName()))) {
            return new TabOrAction("PomView");
        }
        return null;
    }

    private TabOrAction xml(RepoPath repoPath) {
        if (NamingUtils.isXml(repoPath.getName()) && !MavenNaming.isPom((repoPath.getName()))) {
            return IvyNaming.isIvyFileName(repoPath.getName()) ? new TabOrAction("IVYXml") : new TabOrAction("GeneralXml");
        }
        return null;
    }

    private TabOrAction rpm(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".rpm") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.YUM)
                && addonsManager.isAddonSupported(AddonType.YUM)) {
            return new TabOrAction("Rpm");
        }
        return null;
    }

    private TabOrAction nuget(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".nupkg") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.NuGet)
                && addonsManager.isAddonSupported(AddonType.NUGET)) {
            return new TabOrAction("NuPkgInfo");
        }
        return null;
    }

    private TabOrAction gem(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".gem") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Gems)
                && addonsManager.isAddonSupported(AddonType.GEMS)) {
            return new TabOrAction("RubyGems");
        }
        return null;
    }

    private TabOrAction npm(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".tgz") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Npm)
                && addonsManager.isAddonSupported(AddonType.NPM)) {
            return new TabOrAction("NpmInfo");
        }
        return null;
    }

    private TabOrAction debian(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".deb") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Debian)
                && addonsManager.isAddonSupported(AddonType.DEBIAN)) {
            return new TabOrAction("DebianInfo");
        }
        return null;
    }

    private TabOrAction opkg(RepoPath repoPath) {
        if (repoPath.getName().endsWith(".ipk") && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Opkg)
                && addonsManager.isAddonSupported(AddonType.OPKG)) {
            return new TabOrAction("OpkgInfo");
        }
        return null;
    }

    private TabOrAction bower(RepoPath repoPath) {
        boolean isBowerFile = addonsManager.addonByType(BowerAddon.class).isBowerFile(repoPath.getName());
        if (isBowerFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Bower) &&
                addonsManager.isAddonSupported(AddonType.BOWER)) {
            return new TabOrAction("BowerInfo");
        }
        return null;
    }

    private TabOrAction composer(RepoPath repoPath) {
        boolean isComposerFile = addonsManager.addonByType(ComposerAddon.class).isComposerSupportedExtension(repoPath.getName());
        if (isComposerFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Composer) &&
                addonsManager.isAddonSupported(AddonType.COMPOSER)) {
            return new TabOrAction("ComposerInfo");
        }
        return null;
    }

    private TabOrAction chef(RepoPath repoPath) {
        boolean isChefFile = addonsManager.addonByType(ChefAddon.class).isChefCookbookFile(repoPath.getName());
        if (isChefFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Chef) &&
                addonsManager.isAddonSupported(AddonType.CHEF)) {
            return new TabOrAction("ChefInfo");
        }
        return null;
    }

    private TabOrAction pypi(RepoPath repoPath) {
        boolean isPypiFile = addonsManager.addonByType(PypiAddon.class).isPypiFile(repoPath);
        if (isPypiFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Pypi) &&
                addonsManager.isAddonSupported(AddonType.PYPI)) {
            return new TabOrAction("PyPIInfo");
        }
        return null;
    }

    private TabOrAction puppet(RepoPath repoPath) {
        boolean isPuppetFile = addonsManager.addonByType(PuppetAddon.class).isPuppetFile(repoPath);
        if (isPuppetFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Puppet) &&
                addonsManager.isAddonSupported(AddonType.PUPPET)) {
            return new TabOrAction("PuppetInfo");
        }
        return null;
    }

    private TabOrAction helm(RepoPath repoPath) {
        boolean isBowerFile = addonsManager.addonByType(BowerAddon.class).isBowerFile(repoPath.getName());
        if (isBowerFile && nodeRepoTypeHelper.isRepoSupportType(repoPath, RepoType.Helm) &&
                addonsManager.isAddonSupported(AddonType.HELM)) {
            return new TabOrAction("HelmInfo");
        }
        return null;
    }

    private List<TabOrAction> dockerV1(RepoPath repoPath) {
        if (nodeRepoTypeHelper.isDockerFileTypeAndSupported(repoPath)) {
            return Lists.newArrayList(new TabOrAction("DockerInfo"), new TabOrAction("DockerAncestryInfo"));
        }
        return Lists.newArrayList();
    }

    private TabOrAction dockerV2(RepoPath repoPath) {
        if (nodeRepoTypeHelper.isDockerManifestFolder(repoPath)) {
            return new TabOrAction("DockerV2Info");
        }
        return null;
    }

    private TabOrAction conan(RepoPath repoPath) {
        if (nodeRepoTypeHelper.isConanReferenceFolder(repoPath)) {
            return new TabOrAction("ConanInfo");
        } else if (nodeRepoTypeHelper.isConanPackageFolder(repoPath)) {
            return new TabOrAction("ConanPackageInfo");
        }
        return null;
    }

    private TabOrAction general() {
        return new TabOrAction("General");
    }

    private TabOrAction effectivePermissions(RepoPath repoPath) {
        if (authService.canManage(repoPath)) {
            return new TabOrAction("EffectivePermission");
        }
        return null;
    }

    private TabOrAction properties() {
        return new TabOrAction("Properties");
    }

    private TabOrAction watch(RepoPath repoPath) {
        if (authService.canManage(repoPath)) {
            return new TabOrAction("Watch");
        }
        return null;
    }
}

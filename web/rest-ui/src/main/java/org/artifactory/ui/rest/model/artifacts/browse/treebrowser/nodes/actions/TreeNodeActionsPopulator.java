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
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.watch.ArtifactWatchAddon;
import org.artifactory.addon.xray.ArtifactXrayInfo;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.download.FolderDownloadConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.fs.WatchersInfo;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.ui.utils.RegExUtils;
import org.jfrog.client.util.PathUtils;
import org.jfrog.security.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Builds an actions list to be added to a tree node
 *
 * @author Shay Yaakov
 */
@Component
public class TreeNodeActionsPopulator {

    @Autowired
    private RepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private AddonsManager addonsManager;

    public void populateForRepository(TabsAndActions tabsAndActions) {
        List<TabOrAction> actions = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        if ("remote".equals(repoType) || "virtual".equals(repoType)) {
            actions.add(refresh());
            actions.add(virtualZapCaches(tabsAndActions.getRepoType()));
            actions.add(remoteVirtualReindex(tabsAndActions.getRepoPkgType(), tabsAndActions.getRepoType()));
            actions.add(favorites());
        } else if ("distribution".equals(repoType)) {
            actions.add(refresh());
            actions.add(watch(repoPath));
            actions.add(deleteContent(repoPath));
            actions.add(favorites());
        } else if ("trash".equals(repoType)) {
            actions.add(refresh());
            actions.add(searchTrash());
            actions.add(emptyTrash());
        } else {
            actions.add(downloadFolder(repoPath));
            actions.add(refresh());
            actions.add(copyContent(repoPath));
            actions.add(moveContent(repoPath));
            actions.add(watch(repoPath));
            actions.add(zap(repoPath));
            actions.add(reindex(repoPath, tabsAndActions.getRepoPkgType(), tabsAndActions.getRepoType()));
            actions.add(deleteVersions(repoPath));
            actions.add(deleteContent(repoPath));
            actions.add(favorites());
        }
        tabsAndActions.setActions(actions.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public void populateForFolder(TabsAndActions tabsAndActions) {
        List<TabOrAction> actions = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        if ("remote".equals(repoType) || "virtual".equals(repoType)) {
            actions.add(refresh());
        } else if ("distribution".equals(repoType)) {
            actions.add(refresh());
            actions.add(watch(repoPath));
            actions.add(distribute(repoPath));
            actions.add(delete(repoPath));
        } else if ("trash".equals(repoType)) {
            actions.add(refresh());
            actions.add(restore());
            actions.add(deletePermanently());
        } else {
            actions.add(downloadFolder(repoPath));
            actions.add(refresh());
            actions.add(copy(repoPath));
            actions.add(move(repoPath));
            actions.add(watch(repoPath));
            actions.add(distribute(repoPath));
            actions.add(zap(repoPath));
            actions.add(deleteVersions(repoPath));
            actions.add(delete(repoPath));
        }
        tabsAndActions.setActions(actions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public void populateForFile(TabsAndActions tabsAndActions) {
        List<TabOrAction> actions = Lists.newArrayList();
        RepoPath repoPath = tabsAndActions.getRepoPath();
        String repoType = tabsAndActions.getRepoType();
        boolean isCached = tabsAndActions.getCached();
        if ("remote".equals(repoType)) {
            actions.add(download());
        } else if ("virtual".equals(repoType)) {
            if (!isCached) {
                actions.add(download());
                tabsAndActions.setActions(actions.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                return;
            }
            repoPath = repoService.getVirtualFileInfo(repoPath).getRepoPath();
            RepoPath finalRepoPath = repoPath;
            if(repoService.getLocalAndCachedRepoDescriptors().stream()
                    .anyMatch(e -> e.getKey().equals(finalRepoPath.getRepoKey()))) {
                boolean xrayBlocked = addonsManager.addonByType(XrayAddon.class).isDownloadBlocked(repoPath);
                if (!xrayBlocked) {
                    actions.add(download());
                }
            }

        } else if ("distribution".equals(repoType)) {
            actions.add(watch(repoPath));
            actions.add(distribute(repoPath));
            actions.add(delete(repoPath));
            actions.add(download());
        } else if ("trash".equals(repoType)) {
            actions.add(restore());
            actions.add(deletePermanently());
        } else {
            //TODO [by dan]: We must have a global properties object here, can't have each tab going to search for
            //TODO it's own properties when the previous ones already did it - since it's contained to the chosen tab
            //TODO only and I don't have time for it we'll keep it like this for now.
            boolean xrayBlocked = addonsManager.addonByType(XrayAddon.class).isDownloadBlocked(repoPath);
            actions.add(xrayIgnoreUnignore(repoPath, xrayBlocked));
            if (!xrayBlocked) {
                actions.add(download());
                actions.add(distribute(repoPath));
            }
            actions.add(view(repoPath));
            actions.add(copy(repoPath));
            actions.add(move(repoPath));
            actions.add(watch(repoPath));
            actions.add(delete(repoPath));
        }
        tabsAndActions.setActions(actions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    TabOrAction downloadFolder(RepoPath repoPath) {
        FolderDownloadConfigDescriptor folderDownloadConfig = centralConfig.getDescriptor().getFolderDownloadConfig();
        if (authService.canRead(repoPath) && folderDownloadConfig.isEnabled() &&
                (!authService.isAnonymous() || folderDownloadConfig.isEnabledForAnonymous())) {
            return new TabOrAction("DownloadFolder");
        }
        return null;
    }

    private TabOrAction refresh() {
        return new TabOrAction("Refresh");
    }

    private TabOrAction favorites() {
            return new TabOrAction("Favorites");
    }

    private TabOrAction searchTrash() {
        return new TabOrAction("SearchTrash");
    }

    private TabOrAction emptyTrash() {
        return new TabOrAction("EmptyTrash");
    }

    private TabOrAction copyContent(RepoPath repoPath) {
        if (authService.canRead(repoPath) && authService.canDeployToLocalRepository()) {
            return new TabOrAction("CopyContent");
        }
        return null;
    }

    private TabOrAction moveContent(RepoPath repoPath) {
        if (authService.canDelete(repoPath) && authService.canDeployToLocalRepository()) {
            return new TabOrAction("MoveContent");
        }
        return null;
    }

    private TabOrAction watch(RepoPath repoPath) {
        if (userCanWatch(repoPath)) {
            if (isUserWatchingRepoPath(repoPath)) {
                return new TabOrAction("Unwatch");
            } else {
                return new TabOrAction("Watch");
            }
        }
        return null;
    }

    private TabOrAction zap(RepoPath repoPath) {
        LocalRepoDescriptor localRepoDescriptor = localOrCachedRepoDescriptor(repoPath);
        if (authService.canManage(repoPath) && localRepoDescriptor != null && localRepoDescriptor.isCache()) {
            return new TabOrAction("Zap");
        }
        return null;
    }

    private TabOrAction reindex(RepoPath repoPath, RepoType repoPkgType, String repoType) {
        Matcher matcher = RegExUtils.LOCAL_REPO_REINDEX_PATTERN.matcher(repoPkgType.name());
        boolean foundMatch = matcher.matches();
        if (foundMatch && authService.canManage(repoPath) && "local".equals(repoType)) {
            return new TabOrAction("RecalculateIndex");
        }
        return null;
    }

    private TabOrAction deleteVersions(RepoPath repoPath) {
        LocalRepoDescriptor localRepoDescriptor = localOrCachedRepoDescriptor(repoPath);
        if ((authService.canManage(repoPath) || authService.canDelete(repoPath))
                && authService.canRead(repoPath) && localRepoDescriptor != null && localRepoDescriptor.isLocal()) {
            return new TabOrAction("DeleteVersions");
        }
        return null;
    }

    private TabOrAction deleteContent(RepoPath repoPath) {
        if (authService.canDelete(repoPath)) {
            return new TabOrAction("DeleteContent");
        }
        return null;
    }

    private TabOrAction copy(RepoPath repoPath) {
        if (authService.canRead(repoPath) && !NamingUtils.isSystem(repoPath.getPath()) && authService.canDeployToLocalRepository()) {
            return new TabOrAction("Copy");
        }
        return null;
    }

    private TabOrAction move(RepoPath repoPath) {
        if (authService.canDelete(repoPath) && !NamingUtils.isSystem(repoPath.getPath()) && authService.canDeployToLocalRepository()) {
            return new TabOrAction("Move");
        }
        return null;
    }

    private TabOrAction distribute(RepoPath repoPath) {
        // since RTFACT-13636, always visible in menu
        return new TabOrAction("Distribute");
    }

    private TabOrAction delete(RepoPath repoPath) {
        if (authService.canDelete(repoPath)) {
            return new TabOrAction("Delete");
        }
        return null;
    }

    private TabOrAction deletePermanently() {
        if (authService.isAdmin()) {
            return new TabOrAction("DeletePermanently");
        }
        return null;
    }

    private TabOrAction download() {
        return new TabOrAction("Download");
    }

    private TabOrAction view(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (NamingUtils.isViewable(path) || "class".equals(PathUtils.getExtension(path))) {
            return new TabOrAction("View");
        }
        return null;
    }

    private TabOrAction virtualZapCaches(String repoType) {
        if (authService.isAdmin()) {
            if ("virtual".equals(repoType)) {
                return new TabOrAction("ZapCaches");
            }
        }
        return null;
    }

    private TabOrAction remoteVirtualReindex(RepoType repoPkgType, String repoType) {
        if (authService.isAdmin()) {
            if (RegExUtils.REMOTE_REPO_REINDEX_PATTERN.matcher(repoPkgType.name()).matches() && "remote".equals(repoType)) {
                return new TabOrAction("RecalculateIndex");
            } else {
                Matcher matcher = RegExUtils.VIRTUAL_REPO_REINDEX_PATTERN.matcher(repoPkgType.name());
                boolean foundMatch = matcher.matches();
                if (foundMatch && "virtual".equals(repoType)) {
                    return new TabOrAction("RecalculateIndex");
                }
            }
        }
        return null;
    }

    private TabOrAction restore() {
        if (authService.isAdmin()) {
            return new TabOrAction("Restore");
        }
        return null;
    }

    //Adds the ignore/unignore action for admins
    private TabOrAction xrayIgnoreUnignore(RepoPath path, boolean xrayBlocked) {
        if (authService.isAdmin()) {
            ArtifactXrayInfo info = addonsManager.addonByType(XrayAddon.class).getArtifactXrayInfo(path);
            if (xrayBlocked && info.isBlocked() && !info.isAlertIgnored()) {
                //Artifact is blocked - show ignore alert action
                return new TabOrAction("IgnoreAlert");
            } else  if (info.isBlocked() && info.isAlertIgnored()){
                //Artifact not blocked - check if alert is ignored
                return new TabOrAction("UnignoreAlert");
            }
        }
        return null;
    }

    private boolean isUserWatchingRepoPath(RepoPath repoPath) {
        ArtifactWatchAddon watchAddon = addonsManager.addonByType(ArtifactWatchAddon.class);
        return watchAddon.isUserWatchingRepo(repoPath, authService.currentUsername());
    }

    private boolean userCanWatch(RepoPath repoPath) {
        boolean isAddonSupported = addonsManager.isAddonSupported(AddonType.WATCH);
        return authService.canRead(repoPath) && isAddonSupported && !authService.isAnonymous()
                && !authService.isTransientUser() && !isThisBranchHasWatchAlready(repoPath);
    }

    private boolean isThisBranchHasWatchAlready(RepoPath repoPath) {
        ArtifactWatchAddon watchAddon = addonsManager.addonByType(ArtifactWatchAddon.class);
        Pair<RepoPath, WatchersInfo> nearestWatch = watchAddon.getNearestWatchDefinition(repoPath, authService.currentUsername());
        return nearestWatch != null && !(nearestWatch.getFirst().getPath().equals(repoPath.getPath()));
    }

    private LocalRepoDescriptor localOrCachedRepoDescriptor(RepoPath repoPath) {
        return repoService.localOrCachedRepoDescriptorByKey(repoPath.getRepoKey());
    }

    private boolean isDistributionRepo(LocalRepoDescriptor descriptor) {
        return descriptor.getType().equals(RepoType.Distribution);
    }

    private boolean userCanDeployToDistRepo(@Nullable String repoKey) {
        AuthorizationService authService = ContextHelper.get().beanForType(AuthorizationService.class);
        List<String> repoKeys = new ArrayList<>();
        if (StringUtils.isBlank(repoKey)) {
            repoKeys.addAll(repoService.getDistributionRepoDescriptors().stream()
                    .map(RepoDescriptor::getKey)
                    .collect(Collectors.toList()));
        } else {
            repoKeys.add(repoKey);
        }

        return repoKeys.stream()
                .map(key -> RepoPathFactory.create(key, "."))
                .filter(authService::canDeploy)
                .findAny()
                .isPresent();
    }
}

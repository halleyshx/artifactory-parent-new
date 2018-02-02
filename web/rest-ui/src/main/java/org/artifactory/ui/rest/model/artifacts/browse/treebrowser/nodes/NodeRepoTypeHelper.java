package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import org.artifactory.addon.AddonType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.conan.ConanAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.RepoPath;

/**
 * @author Yinon Avraham
 */
public class NodeRepoTypeHelper {

    private final AddonsManager addonsManager;
    private final RepositoryService repoService;

    public NodeRepoTypeHelper(AddonsManager addonsManager, RepositoryService repoService) {
        this.addonsManager = addonsManager;
        this.repoService = repoService;
    }

    public boolean isDockerFileTypeAndSupported(RepoPath repoPath) {
        if (!isDockerSupported(repoPath)) {
            return false;
        }
        return repoService.getChildrenNames(repoPath).contains("json.json");
    }

    public boolean isDockerManifestFolder(RepoPath repoPath) {
        if (!isDockerSupported(repoPath)) {
            return false;
        }
        return repoService.getChildrenNames(repoPath).contains("manifest.json");
    }

    private boolean isDockerSupported(RepoPath repoPath) {
        if (!addonsManager.isAddonSupported(AddonType.DOCKER)) {
            return false;
        }
        return isRepoSupportType(repoPath, RepoType.Docker);
    }

    public boolean isConanReferenceFolder(RepoPath repoPath) {
        boolean conanEnabled = isRepoSupportType(repoPath, RepoType.Conan) && addonsManager.isAddonSupported(AddonType.CONAN);
        ConanAddon conanAddon = addonsManager.addonByType(ConanAddon.class);
        return conanEnabled && conanAddon.isConanReferenceFolder(repoPath);
    }

    public boolean isConanPackageFolder(RepoPath repoPath) {
        boolean conanEnabled = isRepoSupportType(repoPath, RepoType.Conan) && addonsManager.isAddonSupported(AddonType.CONAN);
        ConanAddon conanAddon = addonsManager.addonByType(ConanAddon.class);
        return conanEnabled && conanAddon.isConanPackageFolder(repoPath);
    }

    public boolean isRepoSupportType(RepoPath repoPath, RepoType type) {
        LocalRepoDescriptor descriptor = repoService.localCachedOrDistributionRepoDescriptorByKey(repoPath.getRepoKey());
        return isRepoSupportType(type, descriptor);
    }

    private boolean isRepoSupportType(RepoType type, LocalRepoDescriptor descriptor) {
        return descriptor != null && (descriptor.getType().equals(type) || isDistributionRepo(descriptor));
    }

    private boolean isDistributionRepo(LocalRepoDescriptor descriptor) {
        return descriptor.getType().equals(RepoType.Distribution);
    }

}

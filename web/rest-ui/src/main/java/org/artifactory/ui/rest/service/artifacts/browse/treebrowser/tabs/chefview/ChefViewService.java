package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.chefview;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.chef.ChefAddon;
import org.artifactory.addon.chef.ChefCookbookInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.chef.ChefArtifactMetadataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * Service for fetching the Chef Cookbook metadata for the Info tab in the UI
 *
 * @author Alexis Tual
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ChefViewService implements RestService {

    private AddonsManager addonsManager;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    public ChefViewService(AddonsManager addonsManager) {
        this.addonsManager = addonsManager;
    }

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        BaseArtifactInfo artifactInfo = (BaseArtifactInfo) request.getImodel();
        if(repositoryService.isVirtualRepoExist(artifactInfo.getRepoKey())){
            RepoPath repoPath = InternalRepoPathFactory.create(artifactInfo.getRepoKey(), artifactInfo.getPath());
            repoPath = repositoryService.getVirtualFileInfo(repoPath).getRepoPath();
            artifactInfo.setRepoKey(repoPath.getRepoKey());
        }
        ChefArtifactMetadataInfo cookbookInfo = fetchChefInfo(artifactInfo);
        response.iModel(cookbookInfo);
    }

    /**
     * @param artifactInfo info on the artifact
     * @return a UI model containing Chef Cookbook metadata
     */
    //
    private ChefArtifactMetadataInfo fetchChefInfo(BaseArtifactInfo artifactInfo) {
        ChefAddon chefAddon = addonsManager.addonByType(ChefAddon.class);
        String repoKey = artifactInfo.getRepoKey();
        String path = artifactInfo.getPath();
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
        ChefCookbookInfo chefCookbookInfo = chefAddon.getChefCookbookInfo(repoPath);
        return new ChefArtifactMetadataInfo(chefCookbookInfo);
    }
}

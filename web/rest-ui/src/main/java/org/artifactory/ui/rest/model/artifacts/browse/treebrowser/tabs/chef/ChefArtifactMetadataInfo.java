package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.chef;

import org.artifactory.addon.chef.ChefCookbookInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;

/**
 *
 * The UI model for Chef Cookbook info tab.
 *
 * @author Alexis Tual
 */
public class ChefArtifactMetadataInfo extends BaseArtifactInfo {

    private ChefCookbookInfo chefCookbookInfo;

    public ChefArtifactMetadataInfo(ChefCookbookInfo chefCookbookInfo) {
        this.chefCookbookInfo = chefCookbookInfo;
    }

    public ChefCookbookInfo getChefCookbookInfo() {
        return chefCookbookInfo;
    }

    public void setChefCookbookInfo(ChefCookbookInfo chefCookbookInfo) {
        this.chefCookbookInfo = chefCookbookInfo;
    }
}

package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_COMPOSER_REGISTRY;

/**
 * @author Shay Bagants
 */
public class ComposerTypeSpecificConfigModel extends VcsTypeSpecificConfigModel {

    //remote
    private String registryUrl = DEFAULT_COMPOSER_REGISTRY;

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Composer;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.VCS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}

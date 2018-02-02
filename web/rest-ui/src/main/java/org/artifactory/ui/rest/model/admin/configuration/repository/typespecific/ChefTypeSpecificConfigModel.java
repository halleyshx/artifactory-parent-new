package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.repo.config.RepoConfigDefaultValues;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;
import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_VIRTUAL_RETRIEVAL_CACHE_PERIOD;

/**
 * @author Alexis Tual
 */
public class ChefTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //remote
    protected boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

    //virtual
    private Long virtualRetrievalCachePeriodSecs = DEFAULT_VIRTUAL_RETRIEVAL_CACHE_PERIOD;

    @Override
    public RepoType getRepoType() {
        return RepoType.Chef;
    }

    public boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    public Long getVirtualRetrievalCachePeriodSecs() {
        return virtualRetrievalCachePeriodSecs;
    }

    public void setVirtualRetrievalCachePeriodSecs(Long virtualRetrievalCachePeriodSecs) {
        this.virtualRetrievalCachePeriodSecs = virtualRetrievalCachePeriodSecs;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.CHEF_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }

}

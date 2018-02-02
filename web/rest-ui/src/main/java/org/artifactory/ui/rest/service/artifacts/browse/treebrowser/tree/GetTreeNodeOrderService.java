package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tree;

import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.RestTreeNode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author nadavy
 */
@Component()
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetTreeNodeOrderService implements RestService<RestTreeNode> {
    @Override
    public void execute(ArtifactoryRestRequest<RestTreeNode> request, RestResponse response) {
        response.iModelList(RestTreeNode.getRepoOrder());
    }
}

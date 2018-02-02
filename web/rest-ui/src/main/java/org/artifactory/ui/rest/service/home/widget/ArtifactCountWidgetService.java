package org.artifactory.ui.rest.service.home.widget;

import org.artifactory.api.repo.RepositoryService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.home.HomeWidgetModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Bagants
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ArtifactCountWidgetService implements RestService {

    @Autowired
    private RepositoryService repoService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        HomeWidgetModel model = new HomeWidgetModel("Artifact count Info");
        model.addData("artifactCount", repoService.getArtifactCount());
        response.iModel(model);
    }
}

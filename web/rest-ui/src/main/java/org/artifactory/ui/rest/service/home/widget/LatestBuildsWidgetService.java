package org.artifactory.ui.rest.service.home.widget;

import com.google.common.collect.Lists;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiBuild;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBuild;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.builds.GeneralBuildInfo;
import org.artifactory.ui.rest.model.home.HomeWidgetModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LatestBuildsWidgetService implements RestService {

    @Autowired
    private AqlService aqlService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        HomeWidgetModel widgetModel = new HomeWidgetModel("Latest Builds");
        widgetModel.addData("mostRecentBuilds", getMostRecentBuildsPerPermissions());
        response.iModel(widgetModel);
    }

    /**
     * Return the most recent builds. In case of anonymous user, if the general security configurations disables the
     * build info access for anonymous user, return an empty list.
     */
    private List<GeneralBuildInfo> getMostRecentBuildsPerPermissions() {
        List<GeneralBuildInfo> mostRecentBuilds;
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            mostRecentBuilds = Lists.newArrayList();
        } else {
            AqlEagerResult<AqlBuild> results = aqlService.executeQueryEager(
                    AqlApiBuild.create()
                            .addSortElement(AqlApiBuild.created())
                            .desc()
                            .limit(5));

            mostRecentBuilds = results.getResults().stream()
                    .map(toBuildModel)
                    .collect(Collectors.toList());
        }
        return mostRecentBuilds;
    }

    private final Function<AqlBuild, GeneralBuildInfo> toBuildModel = build ->
            new GeneralBuildInfo.BuildBuilder()
                    .buildName(build.getBuildName())
                    .buildNumber(build.getBuildNumber())
                    .build();
}

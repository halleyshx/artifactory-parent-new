package org.artifactory.ui.rest.resource.onboarding;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.model.onboarding.CreateDefaultReposModel;
import org.artifactory.ui.rest.service.onboarding.OnboardingServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author nadavy
 */
@Path("onboarding")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ArtifactoryOnboardingResource extends BaseResource {

    @Autowired
    private OnboardingServiceFactory onboardingServiceFactory;

    @GET
    @Path("initStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInitStatus() {
        return runService(onboardingServiceFactory.getArtifactoryInitStatusService());
    }

    @GET
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Path("reposStates")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnsetRepos() {
        return runService(onboardingServiceFactory.getUnsetReposService());
    }

    @POST
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Path("createDefaultRepos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDefaultRepos(CreateDefaultReposModel createDefaultReposModel) {
        return runService(onboardingServiceFactory.createDefaultReposService(), createDefaultReposModel);
    }
}

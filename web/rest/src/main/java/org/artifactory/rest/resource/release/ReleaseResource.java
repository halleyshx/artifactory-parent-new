package org.artifactory.rest.resource.release;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.release.bundle.ReleaseBundleAddon;
import org.artifactory.api.rest.constant.HaRestConstants;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.rest.release.ReleaseBundleRequest;
import org.artifactory.api.rest.release.ReleaseBundleResult;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Shay Bagants
 */
@Path(SystemRestConstants.JFROG_RELEASE)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER, HaRestConstants.ROLE_HA})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReleaseResource {

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    AuthorizationService authService;

    @Path("bundle")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @POST
    public Response bundle(ReleaseBundleRequest bundleRequest) {
        if (authService.isAnonymous()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ReleaseBundleAddon releaseBundleAddon = addonsManager.addonByType(ReleaseBundleAddon.class);
        ReleaseBundleResult releaseBundleResult = releaseBundleAddon.handleRequest(bundleRequest);
        return Response.ok().entity(releaseBundleResult).build();
    }
}

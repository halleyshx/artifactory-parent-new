package org.artifactory.ui.rest.resource.admin.configuration.license;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.artifactorylicense.AddClusterLicenseModel;
import org.artifactory.rest.common.model.artifactorylicense.RemoveClusterLicenseModel;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.configuration.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * UI REST-API resource for managing HA cluster licenses in Artifactory
 * Resource endpoints should be available only if HA is **configured** (configured is enough, don't change it to activated)
 *
 * @author Shay Bagants
 */
@Path("manageLicenses")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Produces(MediaType.APPLICATION_JSON)
public class ManageLicensesResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    /**
     * Add new licenses to the cluster
     */
    @Path("add")
    @POST
    public Response addLicenses(AddClusterLicenseModel licenses) {
        return runService(configServiceFactory.addClusterLicensesService(), licenses);
    }

    /**
     * Remove licenses from the cluster. Remove is only optional when the license is currently not in use by any node
     */
    @Path("remove")
    @POST
    public Response deleteLicenses(RemoveClusterLicenseModel licenses) {
        return runService(configServiceFactory.removeClusterLicensesService(), licenses);
    }

    /**
     * Return the entire licenses details.
     */
    @Path("details")
    @GET
    public Response getLicensesDetails() {
        return runService(configServiceFactory.getClusterLicensesService());
    }
}

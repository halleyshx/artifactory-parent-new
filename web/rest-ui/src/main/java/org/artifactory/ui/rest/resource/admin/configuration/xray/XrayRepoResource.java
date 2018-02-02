package org.artifactory.ui.rest.resource.admin.configuration.xray;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.xray.XrayEnabledModel;
import org.artifactory.rest.common.model.xray.XrayRepoModel;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.configuration.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Path("xrayRepo")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class XrayRepoResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @GET
    @Path("getIndex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXrayIndexedRepo() throws Exception {
        return runService(configServiceFactory.getXrayIndexedRepo());
    }

    @GET
    @Path("isXrayEnabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isXrayEnabled() throws Exception {
        return runService(configServiceFactory.isXrayEnabled());
    }

    @POST
    @Path("setXrayEnabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setXrayEnabled(XrayEnabledModel xrayEnabled) throws Exception {
        return runService(configServiceFactory.setXrayEnabled(), xrayEnabled);
    }

    @GET
    @Path("getNoneIndex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNoneXrayIndexedRepo() throws Exception {
        return runService(configServiceFactory.getNoneXrayIndexedRepo());
    }

    @POST
    @Path("addIndex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addXrayIndexedRepo(List<XrayRepoModel> repos) throws Exception {
        return runService(configServiceFactory.addXrayIndexedRepo(), repos);
    }

    @POST
    @Path("removeIndex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeXrayIndexedRepo(List<XrayRepoModel> repos) throws Exception {
        return runService(configServiceFactory.removeXrayIndexedRepo(), repos);
    }

    @PUT
    @Path("indexRepos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectXrayIndexedRepos(List<XrayRepoModel> repos) {
        return runService(configServiceFactory.updateXrayIndexRepos(), repos);
    }
}

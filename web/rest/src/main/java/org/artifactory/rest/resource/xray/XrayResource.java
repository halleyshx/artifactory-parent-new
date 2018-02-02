package org.artifactory.rest.resource.xray;

import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.model.xray.XrayConfigModel;
import org.artifactory.rest.common.model.xray.XrayRepoModel;
import org.artifactory.rest.common.model.xray.XrayScanBuildModel;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.rest.services.ConfigServiceFactory;
import org.artifactory.rest.services.RepoServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * @author Shay Yaakov
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("xray")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class XrayResource extends BaseResource {
    private static final Logger log = LoggerFactory.getLogger(XrayResource.class);
    private static final String anonAccessDisabledMsg = "Anonymous access to build info is disabled";

    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private RepoServiceFactory repoServiceFactory;
    @Autowired
    private ConfigServiceFactory configServiceFactory;
    @Autowired
    private AddonsManager addonsManager;

    @POST
    @Path("index")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response index() throws Exception {
        return runService(repoServiceFactory.indexXray());
    }

    @POST
    @Path("scanBuild")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.WILDCARD)
    @RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
    public Response scanBuild(XrayScanBuildModel xrayScanBuildModel) throws Exception {
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }
        InputStream inputStream = addonsManager.addonByType(XrayAddon.class)
                .scanBuild(XrayResourceHelper.toModel(xrayScanBuildModel));
        /*  if no stream found return error */
        if (inputStream == null) {
            log.error("Scan summary report for build name %s number %s is not available ,check connectivity to Xray",
                    xrayScanBuildModel.getBuildName(), xrayScanBuildModel.getBuildNumber());
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity("Scan summary report is not available")
                    .build();
        }
        /* stream response back to client */
        return XrayResourceHelper.streamResponse(inputStream);
    }

    @DELETE
    @Path("clearAllIndexTasks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAllIndexTasks() throws Exception {
        return runService(repoServiceFactory.clearAllIndexTasks());
    }

    @GET
    @Path("license")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response license() throws Exception {
        return runService(repoServiceFactory.getXrayLicense());
    }

    @GET
    @Path("{repoKey}/indexStats")
    public Response getIndexStats(@PathParam("repoKey") String repoKey) {
        return runService(repoServiceFactory.getXrayIndexStats());
    }

    @Deprecated //backward compatibility, use getXrayIndexedRepoRepos instead
    @GET
    @Path("repos")
    @RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigureReposIndexing() throws Exception {
        return runService(configServiceFactory.getXrayConfiguredRepos());
    }

    @GET
    @Path("indexRepos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXrayIndexedRepo() throws Exception {
        return runService(configServiceFactory.getXrayIndexedRepo());
    }

    @PUT
    @Path("indexRepos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectXrayIndexedRepos(List<XrayRepoModel> repos) {
        return runService(configServiceFactory.updateXrayIndexRepos(), repos);
    }

    @GET
    @Path("nonIndexRepos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNoneXrayIndexedRepo() throws Exception {
        return runService(configServiceFactory.getNoneXrayIndexedRepo());
    }

    @POST
    @Path("setAlertIgnored")
    public Response resetRepoBlocks(@QueryParam("path") String path, @QueryParam("ignore") boolean ignore) throws Exception {
        RepoPath repoPath = RepoPathFactory.create(path);
        addonsManager.addonByType(XrayAddon.class).setAlertIgnored(ignore, repoPath);
        return Response.ok().entity("Ignore alert on path " + path + " set to " + ignore).build();
    }

    //Configuration endpoints

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createXrayConfig(XrayConfigModel xrayConfigModel) throws Exception {
        return runService(configServiceFactory.createXrayConfig(), xrayConfigModel);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateXrayConfig(XrayConfigModel xrayConfigModel) throws Exception {
        return runService(configServiceFactory.updateXrayConfig(), xrayConfigModel);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteXrayConfig() throws Exception {
        return runService(configServiceFactory.deleteXrayConfig());
    }
}

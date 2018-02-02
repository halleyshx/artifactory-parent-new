/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.ui.rest.resource.builds;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.exception.ForbiddenException;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.builds.BuildLicenseModel;
import org.artifactory.ui.rest.model.builds.DeleteBuildsModel;
import org.artifactory.ui.rest.service.builds.BuildsServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@Path("builds")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildsResource extends BaseResource {

    @Autowired
    private BuildsServiceFactory buildsFactory;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    @Qualifier("streamingRestResponse")
    public void setArtifactoryResponse(RestResponse artifactoryResponse) {
        this.artifactoryResponse = artifactoryResponse;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBuilds() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getAllBuilds());
    }

    @GET
    @Path("history{name:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildHistory () throws Exception {
        assertPermissions();
        return runService(buildsFactory.getBuildHistory());
    }

    @GET
    @Path("buildInfo/{name}/{number}{date:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildGeneralInfo() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getBuildGeneralInfo());
    }

    @GET
    @Path("publishedModules/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublishedModules() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getPublishedModules());
    }

    @GET
    @Path("modulesArtifact/{name}/{number}/{date}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getModulesArtifact() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getModuleArtifacts());
    }

    @GET
    @Path("modulesDependency/{name}/{number}/{date}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getModulesDependency() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getModuleDependency());
    }

    @POST
    @Path("buildsDelete")
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBuild(DeleteBuildsModel deleteBuildsModel) throws Exception {
        return runService(buildsFactory.deleteBuild(), deleteBuildsModel);
    }

    @POST
    @Path("deleteAllBuilds")
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllBuild(DeleteBuildsModel deleteBuildsModel) throws Exception {
        return runService(buildsFactory.deleteAllBuilds(), deleteBuildsModel);
    }

    @GET
    @Path("buildJson/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildJson() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getBuildJson());
    }

    @GET
    @Path("artifactDiff/{name}/{number}/{date}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response artifactDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.diffBuildModuleArtifact());
    }

    @GET
    @Path("buildArtifactDiff/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildArtifactDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.diffBuildArtifact());
    }

    @GET
    @Path("buildDependencyDiff/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildDependencyDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.diffBuildDependencies());
    }

    @GET
    @Path("buildPropsDiff/{name}/{number}/{date}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildPropsDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.diffBuildProps());
    }

    @GET
    @Path("buildProps/env/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnvBuildProps() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getEnvBuildProps());
    }

    @GET
    @Path("buildDiff/{name}/{number}/{date}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.buildDiff());
    }

    @GET
    @Path("buildProps/system/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemBuildProps() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getSystemBuildProps());
    }

    @GET
    @Path("buildIssues/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildIssues() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getBuildIssues());
    }

    @GET
    @Path("buildLicenses/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildLicense() throws Exception {
        assertPermissions();
        return runService(buildsFactory.buildLicenses());
    }

    @POST
    @Path("exportLicenses")
    @Consumes("application/x-www-form-urlencoded")
    public Response exportLicense(@FormParam("data") String data) throws Exception {
        assertPermissions();
        BuildLicenseModel buildLicenseModel = JsonUtil.mapDataToModel(data, BuildLicenseModel.class);
        return runService(buildsFactory.exportLicenseToCsv(), buildLicenseModel);
    }

    @PUT
    @Path("overrideLicenses/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response overrideLicense(BuildLicenseModel buildLicenseModel) throws Exception {
        assertPermissions();
        return runService(buildsFactory.overrideSelectedLicenses(), buildLicenseModel);
    }

    @GET
    @Path("changeLicenses/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChangeLicenseValues() throws Exception {
        assertPermissions();
        return runService(buildsFactory.changeBuildLicense());
    }

    @GET
    @Path("releaseHistory/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildReleaseHistory() throws Exception {
        assertPermissions();
        return runService(buildsFactory.buildReleaseHistory());
    }

    @GET
    @Path("dependencyDiff/{name}/{number}/{date}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dependencyDiff() throws Exception {
        assertPermissions();
        return runService(buildsFactory.diffBuildModuleDependency());
    }

    @GET
    @Path("prevBuild/{name}/{number}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrevBuild() throws Exception {
        assertPermissions();
        return runService(buildsFactory.getPrevBuildList());
    }

    private void assertPermissions() {
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new ForbiddenException();
        }
    }
}

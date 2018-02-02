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

package org.artifactory.ui.rest.resource.admin.configuration.generalconfiguration;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.ui.rest.model.admin.configuration.generalconfig.GeneralConfig;
import org.artifactory.ui.rest.model.utils.FileUpload;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.configuration.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * @author Chen Keinan
 */
@Path("generalConfig")
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GeneralConfigurationResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @Autowired
    @Qualifier("streamingRestResponse")
    public void setArtifactoryResponse(RestResponse artifactoryResponse) {
        this.artifactoryResponse = artifactoryResponse;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneralConfig()
            throws Exception {
        return runService(configServiceFactory.getGeneralConfig());
    }

    @GET
    @Path("data")
    @RolesAllowed({AuthorizationService.ROLE_ADMIN,AuthorizationService.ROLE_USER})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneralConfigData()
            throws Exception {
        return runService(configServiceFactory.getGeneralConfigData());
    }

    @POST
    @Path("logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadLogo(FormDataMultiPart formDataMultiPart) throws Exception {
        FileUpload fileUpload = new FileUpload(formDataMultiPart);
        return runService(configServiceFactory.uploadLogo(), fileUpload);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveGeneralConfig(GeneralConfig generalConfig)
            throws Exception {
        return runService(configServiceFactory.updateGeneralConfig(), generalConfig);
    }

    @GET
    @Produces("image/*")
    @Path("logo")
    @RolesAllowed({AuthorizationService.ROLE_ADMIN,AuthorizationService.ROLE_USER})
    public Response getUploadLogo() throws Exception {
        return runService(configServiceFactory.getUploadLogo());
    }

    @DELETE
    @Path("logo")
    public Response deleteUploadLogo() throws Exception {
        return runService(configServiceFactory.deleteUploadedLogo());
    }
}

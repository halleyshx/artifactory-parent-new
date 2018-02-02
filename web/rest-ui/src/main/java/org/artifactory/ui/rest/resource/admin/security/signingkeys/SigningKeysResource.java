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

package org.artifactory.ui.rest.resource.admin.security.signingkeys;

import com.sun.jersey.multipart.FormDataMultiPart;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.ui.rest.model.admin.security.signingkey.SignKey;
import org.artifactory.ui.rest.model.utils.FileUpload;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("signingkeys")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SigningKeysResource extends BaseResource {

    @Autowired
    private SecurityServiceFactory securityFactory;

    @POST
    @Path("install")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSigningKey(FormDataMultiPart formParams)
            throws Exception {
        FileUpload fileUpload = new FileUpload(formParams);
        return runService(securityFactory.uploadSigningKey(), fileUpload);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeSigningKey()
            throws Exception {
        return runService(securityFactory.removeSigningKeyService());
    }


    @POST
    @Path("verify")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifySigningKey(SignKey signKey)
            throws Exception {
        return runService(securityFactory.verifySigningKey(), signKey);
    }

    @PUT
    @Path("update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePassPhrase(SignKey signKey)
            throws Exception {
        return runService(securityFactory.updateSigningKey(), signKey);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSigningKey()
            throws Exception {
        return runService(securityFactory.getSigningKey());
    }
}

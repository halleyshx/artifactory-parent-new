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

package org.artifactory.ui.rest.resource.admin.configuration.reverseProxy;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.rest.common.model.reverseproxy.ReverseProxyDescriptorModel;
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

/**
 * @author Shay Yaakov
 */
@Path("reverseProxies")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReverseProxyResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createReverseProxy(ReverseProxyDescriptorModel reverseProxy) throws Exception {
        return runService(configServiceFactory.createReverseProxy(), reverseProxy);
    }


    @PUT
    @Path("crud{id:(/[^/]+?)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateReverseProxy(ReverseProxyDescriptor reverseProxy) throws Exception {
        return runService(configServiceFactory.updateReverseProxy(), reverseProxy);
    }

    @GET
    @Path("crud{id:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReverseProxies() throws Exception {
        return runService(configServiceFactory.getReverseProxies());
    }

    @GET
    @Path("checkPort{port:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkReverseProxyPortAvailability() throws Exception {
        return runService(configServiceFactory.checkReverseProxyPortAvailability());
    }
}

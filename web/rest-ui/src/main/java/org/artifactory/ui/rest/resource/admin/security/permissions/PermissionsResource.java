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

package org.artifactory.ui.rest.resource.admin.security.permissions;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.model.admin.security.permissions.DeletePermissionTargetModel;
import org.artifactory.ui.rest.model.admin.security.permissions.PermissionTargetModel;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Path("targetPermissions")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PermissionsResource extends BaseResource {

    @Autowired
    private SecurityServiceFactory securityFactory;

    @GET
    @Path("crud{name:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPermissionTarget()
            throws Exception {
        return runService(securityFactory.getPermissionsTarget());
    }

    @GET
    @Path("allUsersGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsersGroups() throws Exception {
        return runService(securityFactory.getAllUsersAndGroups());
    }

    @GET
    @RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{username}")
    public Response getEffectivePermissionsByUser() {
        return runService(securityFactory.getGetEffectivePermissionServiceByEntity());
    }

    @GET
    @RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("groups/{groupname}")
    public Response getEffectivePermissionsByGroup() {
        return runService(securityFactory.getGetEffectivePermissionServiceByEntity());
    }

    @PUT
    @Path("{name : [^/]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePermissionTarget(PermissionTargetModel permissionTargetModel)
            throws Exception {
        return runService(securityFactory.updatePermissionsTarget(), permissionTargetModel);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response createPermissionTarget(PermissionTargetModel permissionTargetModel)
            throws Exception {
        return runService(securityFactory.createPermissionsTarget(), permissionTargetModel);
    }

    @POST
    @Path("deleteTargetPermissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePermissionTarget(DeletePermissionTargetModel deletePermissionTargetModel) throws Exception {
        return runService(securityFactory.deletePermissionsTarget(), deletePermissionTargetModel);
    }
}

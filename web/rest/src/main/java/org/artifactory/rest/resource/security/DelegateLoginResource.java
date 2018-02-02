package org.artifactory.rest.resource.security;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.rest.constant.SecurityRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Path(SecurityRestConstants.PATH_ROOT + "/auth")
public class DelegateLoginResource {
    private static final Logger log = LoggerFactory.getLogger(DelegateLoginResource.class);

    private static final String AUTH_ERR = "{\"error\" : \"'%s'\"}";
    private static final String ERR = "{\"error\" : \"%s\"}";

    @Autowired
    private AddonsManager addonsManager;

    @Context
    private HttpServletRequest request;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response getUsersInfo(UserAuthDetails userAuthDetails) {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        AuthDelegationHandler authDelegationHandler = restAddon.getAuthDelegationHandler(request);
        try {
            return authDelegationHandler.handleRequest(userAuthDetails);
        } catch (AuthenticationException auth) {
            log.debug("", auth);
            return Response.ok().entity(String.format(AUTH_ERR, auth.getMessage())).build();
        } catch (Exception e) {
            log.debug("", e);
            return Response.status(SC_BAD_REQUEST).entity(String.format(ERR, e.getMessage())).build();
        }
    }
}
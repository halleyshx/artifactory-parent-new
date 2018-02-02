package org.artifactory.rest.resource.token;

import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.exception.GlobalExceptionMapper;
import org.artifactory.security.access.AccessService;
import org.jfrog.access.client.token.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static org.artifactory.api.security.AuthorizationService.ROLE_ADMIN;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(AccessAdminTokenResource.PATH_ROOT)
@RolesAllowed({ROLE_ADMIN})
public class AccessAdminTokenResource {

    public static final String PATH_ROOT = "security/access/admin/token";

    private static final Logger log = LoggerFactory.getLogger(AccessAdminTokenResource.class);

    private final AccessService accessService;

    @Autowired
    public AccessAdminTokenResource(AccessService accessService) {
        this.accessService = accessService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccessAdminToken(AccessAdminTokenModel model) {
        try {
            TokenResponse tokenWithAdminCredentials = accessService
                    .createTokenWithAccessAdminCredentials(model.getServiceId());
            return Response.ok(tokenWithAdminCredentials).build();
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Failed to create token With Admin Credentials ", e);
            throw new BadRequestException("Failed to create token with Admin Credentials");
        }
    }
}

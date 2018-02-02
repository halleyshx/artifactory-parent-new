package org.artifactory.security.access;

import org.artifactory.security.SimpleUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @author Noam Shemesh
 */
public class AccessResponseAuthentication extends AbstractAuthenticationToken {
    private SimpleUser principal;

    public AccessResponseAuthentication(boolean authenticated, SimpleUser simpleUser) {
        super(simpleUser.isEffectiveAdmin() ? SimpleUser.ADMIN_GAS : SimpleUser.USER_GAS);
        this.principal = simpleUser;
        setAuthenticated(authenticated);
    }

    @Override
    public String getCredentials() {
        return principal.getPassword();
    }

    @Override
    public SimpleUser getPrincipal() {
        return principal;
    }
}

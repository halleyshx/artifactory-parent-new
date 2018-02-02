package org.artifactory.security;

import org.artifactory.request.Request;
import org.springframework.security.core.AuthenticationException;

/**
 * @author nadavy
 */
public interface PluggableAuthenticationProviderIfc extends RealmAwareAuthenticationProvider {

    void authenticateAdditive(Request request) throws AuthenticationException;
}

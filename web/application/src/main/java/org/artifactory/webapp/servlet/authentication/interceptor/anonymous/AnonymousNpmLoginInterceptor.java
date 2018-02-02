package org.artifactory.webapp.servlet.authentication.interceptor.anonymous;

import javax.servlet.http.HttpServletRequest;

/**
 * This interceptor allows anonymous access for the NPM client login process (npm login command)
 * Since the login request has no headers of authentication, we need to parse the login information from the JSON.
 * See NpmResource class.
 *
 * @author Yuval Reches
 */
public class AnonymousNpmLoginInterceptor implements AnonymousAuthenticationInterceptor {

    @Override
    public boolean accept(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.matches(".*npm\\/.*\\/-\\/user\\/.*");
    }
}

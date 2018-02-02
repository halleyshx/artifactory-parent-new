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

package org.artifactory.addon.sso.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;

/**
 * @author Gidi Shabat
 */
public interface SamlHandler {

    /**
     * Check if the authentication is SAML authentication.
     */
    public boolean isSamlAuthentication(HttpServletRequest request, HttpServletResponse response) throws SamlException;

    /**
     * Handle identityProvider login response.
     */
    public void handleLoginResponse(HttpServletRequest request, HttpServletResponse response,
            HashMap<String, List<String>> samlResponseFromParam) throws SamlException;
    /**
     * Handle login event from Artifactory login link.
     * Create login request and redirect to the  identity provider.
     */
    void handleLoginRequest(HttpServletRequest request, HttpServletResponse response) throws SamlException;

    /**
     * Return the logout redirect url
     */
    public String generateSamlLogoutRedirectUrl(HttpServletRequest request, HttpServletResponse response)throws SamlException;
}

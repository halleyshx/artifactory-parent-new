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

package org.artifactory.security;

import com.google.common.collect.Sets;
import org.artifactory.api.rest.constant.HaRestConstants;
import org.artifactory.api.security.SecurityService;
import org.artifactory.factory.InfoFactoryHolder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Set;

/**
* Authentication token for HA rest resources.
* <p>see {@link HaRestConstants#ROLE_HA}
*
* @author mamo
*/
public class HaSystemAuthenticationToken extends AbstractAuthenticationToken implements Serializable {

    public static final Set<GrantedAuthority> HA_ADMIN_GAS =
            Sets.newHashSet(new SimpleGrantedAuthority(HaRestConstants.ROLE_HA));

    /**
     * Creates a token with the supplied array of authorities.
     */
    public HaSystemAuthenticationToken() {
        super(HA_ADMIN_GAS);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        MutableUserInfo user = InfoFactoryHolder.get().createUser(SecurityService.USER_SYSTEM);
        user.setAdmin(true);
        return new SimpleUser(user);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}

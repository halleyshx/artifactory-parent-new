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

import com.google.common.collect.Maps;
import org.artifactory.api.security.UserAwareAuthenticationProvider;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.factory.InfoFactoryHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An authentication manager that has realm-aware auth providers.
 *
 * @author Tomer Cohen
 */
public class RealmAwareAuthenticationManager extends ProviderManager implements UserAwareAuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(RealmAwareAuthenticationManager.class);

    @Autowired
    private UserGroupService userGroupService;

    private Map<String, RealmAwareAuthenticationProvider> realmAwareAuthenticationProviders = Maps.newHashMap();

    public RealmAwareAuthenticationManager(List<AuthenticationProvider> providers) {
        super(providers);
        providers.stream()
                .filter(provider -> provider instanceof RealmAwareAuthenticationProvider)
                .forEach(provider -> addRealmAwareAuthenticationProvider((RealmAwareAuthenticationProvider) provider));
    }

    /**
     * {@inheritDoc}
     * <p/>
     * After the user has been authenticated, it will be updated with the appropriate <i>realm</i> that it came from.
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = super.authenticate(authentication);
        if (result instanceof RealmAwareAuthentication) {
            RealmAwareAuthentication realmAwareAuthentication = (RealmAwareAuthentication) result;
            String realm = realmAwareAuthentication.getRealm();
            log.debug("Authentication '{}' has realm '{}'", authentication, realm);
            UserInfo user =
                    userGroupService.findOrCreateExternalAuthUser(authentication.getPrincipal().toString(), true);
            if (!user.isTransientUser() && !user.isAnonymous() && !realm.equals(user.getRealm())) {
                MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
                mutableUser.setRealm(realm);
                log.debug("Updating user '{}' with realm '{}'", user, realm);
                // lock the update transaction.
                // If trying to access this concurrently, a TransactionSystemException may occur.
                // see RTFACT-3883
                synchronized (this) {
                    userGroupService.updateUser(mutableUser, false);
                }
            }
        }
        return result;
    }

    @Override
    public boolean userExists(String userName, String realm) {
        RealmAwareAuthenticationProvider provider = getAuthenticationProvider(realm);
        return provider != null && provider.userExists(userName);
    }

    @Override
    public void addExternalGroups(String userName, String realm, Set<UserGroupInfo> groups) {
        RealmAwareAuthenticationProvider provider = getAuthenticationProvider(realm);
        if (provider == null) {
            return;
        }
        provider.addExternalGroups(userName, groups);
    }

    public void addRealmAwareAuthenticationProvider(RealmAwareAuthenticationProvider provider) {
        realmAwareAuthenticationProviders.put(provider.getRealm(), provider);
    }

    /**
     * Return a list of PluggableAuthenticationProvider providers for applying closures
     */
    public List<PluggableAuthenticationProviderIfc> getPluggableAuthenticationProviders() {
        // Find plugin provider
        return getProviders().stream()
                .filter(provider->PluggableAuthenticationProviderIfc.class.isAssignableFrom(provider.getClass()))
                .map(provider->(PluggableAuthenticationProviderIfc) provider)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RealmAwareAuthenticationProvider getAuthenticationProvider(String realm) {
        return realmAwareAuthenticationProviders.get(realm);
    }
}

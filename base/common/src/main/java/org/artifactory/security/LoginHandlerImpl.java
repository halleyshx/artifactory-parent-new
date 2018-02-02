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

import com.google.common.collect.Lists;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.access.CreatedTokenInfo;
import org.artifactory.api.security.access.UserTokenSpec;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.security.access.AccessService;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.model.AuthenticationModel;
import org.artifactory.security.props.auth.model.OauthModel;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.dateUtils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Chen  Keinan
 */
@Component
public class LoginHandlerImpl implements LoginHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginHandlerImpl.class);

    @Autowired
    private OauthManager oauthManager;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AccessService accessService;

    @Override
    public OauthModel doBasicAuthWithDb(String[] tokens,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) throws IOException, ParseException {
        assert tokens.length == 2;
        AuthenticationManager authenticationManager = ContextHelper.get().beanForType(AuthenticationManager.class);
        String username = tokens[0];
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, tokens[1]);
        authRequest.setDetails(authenticationDetailsSource);
        Authentication authenticate = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        TokenKeyValue tokenKeyValue = oauthManager.getToken(username);
        if (tokenKeyValue == null) {
            tokenKeyValue = oauthManager.createToken(username);
        }
        boolean externalUserToken = false;
        if (tokenKeyValue == null) {
            log.debug("could not create and persist token for authenticated user {}, storing generated token in shared cache.", username);
            tokenKeyValue = generateToken(((UserDetails) authenticate.getPrincipal()).getUsername());
            if (tokenKeyValue == null) {
                throw new RuntimeException("failed to generate token for authenticated user: " + username);
            }
            externalUserToken = true;
        }
        AuthenticationModel oauthModel = new AuthenticationModel(tokenKeyValue.getToken(), DateUtils.formatBuildDate(System.currentTimeMillis()));
        if (externalUserToken) {
            oauthModel.setExpiresIn(ConstantValues.genericTokensCacheIdleTimeSecs.getInt());
        }
        return oauthModel;
    }

    public TokenKeyValue generateToken(String userName) {
        TokenKeyValue token = null;
        CreatedTokenInfo createdTokenInfo;
        String key = "accesstoken";
        try {
            UserInfo userInfo = userGroupService.currentUser();
            String scope = getScope(userInfo);
            UserTokenSpec tokenSpec = UserTokenSpec.create(userName)
                    .expiresIn(ConstantValues.genericTokensCacheIdleTimeSecs.getLong())
                    .refreshable(false)
                    .scope(Lists.newArrayList(scope));
            createdTokenInfo = accessService.createToken(tokenSpec);
            token = new TokenKeyValue(key, createdTokenInfo.getTokenValue());
        } catch (Exception e) {
            log.debug("Failed generating token for user '{}' with key '{}'. {}", userName, key, e.getMessage());
            log.trace("Failed generating token.", e);
        }
        return token;
    }

    private String getScope(UserInfo userInfo) {
        StringBuilder builder = new StringBuilder("member-of-groups:");
        Set<UserGroupInfo> groups = userInfo.getGroups();
        if (CollectionUtils.isNullOrEmpty(groups)) {
            builder.append("*");
        } else {
            Iterator<UserGroupInfo> it = groups.iterator();
            boolean hasNext = it.hasNext();
            while (hasNext) {
                builder.append(it.next());
                if (hasNext = it.hasNext()) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }

    @Override
    public OauthModel doBasicAuthWithProvider(String header, String username) {
        OAuthHandler oAuthHandler = ContextHelper.get().beanForType(OAuthHandler.class);
        CentralConfigDescriptor descriptor = ContextHelper.get().getCentralConfig().getDescriptor();
        OAuthSettings oauthSettings = descriptor.getSecurity().getOauthSettings();
        String defaultProvider = oauthSettings.getDefaultNpm();
        // try to get token from provider
        return oAuthHandler.getCreateToken(defaultProvider, username, header);
    }

    @Override
    public String[] extractAndDecodeHeader(String header) throws IOException {
        byte[] base64Token = header.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = org.springframework.security.crypto.codec.Base64.decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token = new String(decoded, "UTF-8");

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }
}

package org.artifactory.rest.common.access;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.*;
import org.artifactory.rest.common.exception.ForbiddenException;
import org.artifactory.security.access.AccessTokenAuthentication;
import org.jfrog.access.token.JwtAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Yinon Avraham.
 */
public class AccessTokenScopeResourceFilterFactory implements ResourceFilterFactory {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenScopeResourceFilterFactory.class);
    /**
     * Cache of parse results of path pattern scope token. Caches both successes and failures to parse a token by caching
     * optionals with the compiled pattern for successfully parsed tokens and empty optionals for failures.
     */
    private static final LoadingCache<String, Optional<RestPathScopePattern>> PATH_PATTERNS = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build(new CacheLoader<String, Optional<RestPathScopePattern>>() {
                @Override
                public Optional<RestPathScopePattern> load(@Nonnull String scopeToken) throws Exception {
                    return RestPathScopePattern.parseOptional(scopeToken);
                }
            });
    private static final List<ResourceFilter> FILTERS = Collections.singletonList(new Filter());

    @Override
    public List<ResourceFilter> create(AbstractMethod abstractMethod) {
        return FILTERS;
    }

    private static class Filter implements ResourceFilter, ContainerRequestFilter {

        public Filter() { }

        @Override
        public ContainerRequest filter(ContainerRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof AccessTokenAuthentication) {
                JwtAccessToken accessToken = ((AccessTokenAuthentication) authentication).getAccessToken();
                checkRequestPath(accessToken, request);
            }
            return request;
        }

        private void checkRequestPath(JwtAccessToken accessToken, ContainerRequest request) {
            if (!requestPathMatchesAnyPathPatternScope(request, accessToken)) {
                if (log.isDebugEnabled()) {
                    log.debug("Request failed token authorization - " +
                            "request path '{}' does not match allowed patterns in the token scope: {}",
                            request.getAbsolutePath(), String.join(" ", accessToken.getScope()));
                }
                throw new ForbiddenException("Request path not allowed");
            }
        }

        private boolean requestPathMatchesAnyPathPatternScope(ContainerRequest request, JwtAccessToken accessToken) {
            return accessToken.getScope().stream().anyMatch(scopeToken -> {
                try {
                    Optional<RestPathScopePattern> pathScopePattern = PATH_PATTERNS.get(scopeToken);
                    return pathScopePattern.map(pattern -> pattern.matches(request)).orElse(false);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public ContainerRequestFilter getRequestFilter() {
            return this;
        }

        @Override
        public ContainerResponseFilter getResponseFilter() {
            //Response filtering is not needed
            return null;
        }
    }

}

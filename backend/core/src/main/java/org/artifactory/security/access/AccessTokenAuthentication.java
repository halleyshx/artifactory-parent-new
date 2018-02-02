package org.artifactory.security.access;

import org.jfrog.access.token.JwtAccessToken;
import org.jfrog.access.token.JwtAccessTokenImpl;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Yinon Avraham
 */
public class AccessTokenAuthentication extends AbstractAuthenticationToken {

    private final JwtAccessToken accessToken;
    //TODO [YA] remove the principal once supporting access token authorization. Then the principal is actually the token.
    private final Object principal;

    /**
     * Creates a token with the supplied array of authorities.
     * @param tokenValue  the access token represented by this authentication.
     * @param principal   the principal represented by this authentication.
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the
     * @throws IllegalArgumentException if the given token value is not in the expected format.
     */
    public AccessTokenAuthentication(@Nonnull String tokenValue, @Nullable Object principal,
            @Nullable Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.accessToken = JwtAccessTokenImpl.parseTokenValue(tokenValue);
        this.principal = principal;
    }

    /**
     * Creates a token with the supplied array of authorities.
     * @param accessToken the access token represented by this authentication.
     * @param principal   the principal represented by this authentication.
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the
     */
    public AccessTokenAuthentication(@Nonnull JwtAccessToken accessToken, @Nullable Object principal,
            @Nullable Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.accessToken = accessToken;
        this.principal = principal;
    }

    @Nonnull
    public JwtAccessToken getAccessToken() {
        return accessToken;
    }

    @Override
    public Object getCredentials() {
        return accessToken.getTokenValue();
    }

    @Nullable
    @Override
    public Object getPrincipal() {
        return principal;
    }
}

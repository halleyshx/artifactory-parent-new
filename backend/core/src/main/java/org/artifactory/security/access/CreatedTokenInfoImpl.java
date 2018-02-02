package org.artifactory.security.access;

import org.artifactory.api.security.access.CreatedTokenInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Yinon Avraham
 */
class CreatedTokenInfoImpl implements CreatedTokenInfo {

    private final String tokenValue;
    private final String tokenType;
    private final String refreshToken;
    private final String scope;
    private final Long expiresIn;

    CreatedTokenInfoImpl(@Nonnull String tokenValue, @Nonnull String tokenType, @Nullable String refreshToken,
            @Nonnull String scope, @Nullable Long expiresIn) {
        this.tokenValue = tokenValue;
        this.tokenType = tokenType;
        this.refreshToken = refreshToken;
        this.scope = scope;
        this.expiresIn = expiresIn;
    }

    @Nonnull
    @Override
    public String getTokenValue() {
        return tokenValue;
    }

    @Nonnull
    @Override
    public String getTokenType() {
        return tokenType;
    }

    @Nullable
    @Override
    public String getRefreshToken() {
        return refreshToken;
    }

    @Nonnull
    @Override
    public String getScope() {
        return scope;
    }

    @Nullable
    @Override
    public Long getExpiresIn() {
        return expiresIn;
    }
}

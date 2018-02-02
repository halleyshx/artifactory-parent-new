package org.artifactory.api.security.access;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Yinon Avraham
 */
public interface CreatedTokenInfo {

    @Nonnull
    String getTokenValue();

    @Nonnull
    String getTokenType();

    @Nullable
    String getRefreshToken();

    @Nonnull
    String getScope();

    @Nullable
    Long getExpiresIn();

}

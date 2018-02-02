package org.artifactory.api.security.access;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Yinon Avraham.
 */
public interface TokenInfo {

    @Nonnull
    String getTokenId();

    @Nonnull
    String getIssuer();

    @Nonnull
    String getSubject();

    @Nullable
    Long getExpiry();

    long getIssuedAt();

    boolean isRefreshable();

}

package org.artifactory.security.access;

import org.artifactory.api.security.access.TokenInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author Yinon Avraham.
 */
public class TokenInfoImpl implements TokenInfo {

    private final String tokenId;
    private final String issuer;
    private final String subject;
    private final Long expiry;
    private final long issuedAt;
    private final boolean refreshable;

    public TokenInfoImpl(@Nonnull String tokenId, @Nonnull String issuer, @Nonnull String subject,
            @Nullable Long expiry, long issuedAt, boolean refreshable) {
        this.tokenId = requireNonBlank(tokenId, "tokenId is required");
        this.issuer =  requireNonBlank(issuer, "issuer is required");
        this.subject =  requireNonBlank(subject, "subject is required");
        this.expiry = requireNonNegativeOrNull(expiry, "expiry must be null or non-negative");
        this.issuedAt = requireNonNegative(issuedAt, "issuedAt must be non-negative");
        this.refreshable = refreshable;
    }

    @Nonnull
    @Override
    public String getTokenId() {
        return tokenId;
    }

    @Nonnull
    @Override
    public String getIssuer() {
        return issuer;
    }

    @Nonnull
    @Override
    public String getSubject() {
        return subject;
    }

    @Nullable
    @Override
    public Long getExpiry() {
        return expiry;
    }

    @Override
    public long getIssuedAt() {
        return issuedAt;
    }

    @Override
    public boolean isRefreshable() {
        return refreshable;
    }

    @Override
    public String toString() {
        return "TokenInfoImpl{" +
                "tokenId='" + tokenId + '\'' +
                ", issuer='" + issuer + '\'' +
                ", subject='" + subject + '\'' +
                ", expiry=" + expiry +
                ", issuedAt=" + issuedAt +
                ", refreshable=" + refreshable +
                '}';
    }

    private static String requireNonBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Long requireNonNegativeOrNull(Long value, String message) {
        if (value != null) {
            return requireNonNegative(value, message);
        }
        return null;
    }

    private static long requireNonNegative(Long value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}

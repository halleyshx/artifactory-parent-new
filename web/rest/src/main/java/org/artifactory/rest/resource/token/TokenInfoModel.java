package org.artifactory.rest.resource.token;

import org.artifactory.api.security.access.TokenInfo;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * @author Yinon Avraham.
 */
public class TokenInfoModel {

    @JsonProperty("token_id")
    private String tokenId;
    @JsonProperty("issuer")
    private String issuer;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("issued_at")
    private long issuedAt;
    @JsonProperty("expiry")
    private Long expiry;
    @JsonProperty("refreshable")
    private boolean refreshable;

    public TokenInfoModel() { }

    public TokenInfoModel(@Nonnull TokenInfo tokenInfo) {
        this.tokenId = tokenInfo.getTokenId();
        this.issuer = tokenInfo.getIssuer();
        this.subject = tokenInfo.getSubject();
        this.issuedAt = toEpochInSecs(tokenInfo.getIssuedAt());
        this.expiry = toEpochInSecs(tokenInfo.getExpiry());
        this.refreshable = tokenInfo.isRefreshable();
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getExpiry() {
        return expiry;
    }

    public void setExpiry(Long expiry) {
        this.expiry = expiry;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public void setRefreshable(boolean refreshable) {
        this.refreshable = refreshable;
    }

    private Long toEpochInSecs(Long epochInMillis) {
        return epochInMillis == null ? null : TimeUnit.MILLISECONDS.toSeconds(epochInMillis);
    }
}

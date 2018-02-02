package org.artifactory.ui.rest.model.admin.security.accesstokens;

import org.artifactory.api.security.access.TokenInfo;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.rest.common.util.RestUtils;

import static org.artifactory.api.security.access.UserTokenSpec.extractUsername;
import static org.artifactory.api.security.access.UserTokenSpec.isUserTokenSubject;

/**
 * @author Yinon Avraham.
 */
public class AccessTokenUIModel extends BaseModel {

    private String tokenId;
    private String issuer;
    private String subject;
    private String issuedAt; //ISO8601 date format
    private String expiry; //ISO8601 date format
    private boolean refreshable;

    public AccessTokenUIModel() {}

    public AccessTokenUIModel(TokenInfo tokenInfo) {
        this.tokenId = tokenInfo.getTokenId();
        this.issuer = tokenInfo.getIssuer();
        this.subject = isUserTokenSubject(tokenInfo.getSubject()) ?
                extractUsername(tokenInfo.getSubject()) :
                tokenInfo.getSubject();
        this.issuedAt = toIsoDate(tokenInfo.getIssuedAt());
        this.expiry = toIsoDate(tokenInfo.getExpiry());
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

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAtIsoDate) {
        this.issuedAt = issuedAtIsoDate;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiryIsoDate) {
        this.expiry = expiryIsoDate;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public void setRefreshable(boolean refreshable) {
        this.refreshable = refreshable;
    }

    private static String toIsoDate(Long timeInMillis) {
        return timeInMillis == null ? null : RestUtils.toIsoDateString(timeInMillis);
    }
}

package org.artifactory.rest.resource.token;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * @author Yinon Avraham
 */
public enum TokenType {

    AccessToken("access_token"),
    RefreshToken("refresh_token");

    private final String signature;

    TokenType(@Nonnull String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return signature;
    }

    @Nonnull
    public static TokenType fromSignature(@Nonnull String signature) {
        return Stream.of(values())
                .filter(grantType -> grantType.getSignature().equals(signature))
                .findFirst()
                .orElseThrow(() ->
                        new TokenRequestException(TokenResponseErrorCode.InvalidRequest,
                                "Token type is not supported: " + signature)
                );
    }
}

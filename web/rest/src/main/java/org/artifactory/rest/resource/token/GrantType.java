package org.artifactory.rest.resource.token;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * @author Yinon Avraham
 */
public enum GrantType {

    ClientCredentials("client_credentials"),
    RefreshToken("refresh_token"),
    Password("password");

    private final String signature;

    GrantType(@Nonnull String signature) {
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
    public static GrantType fromSignature(@Nonnull String signature) {
        return Stream.of(values())
                .filter(grantType -> grantType.getSignature().equals(signature))
                .findFirst()
                .orElseThrow(() -> new GrantTypeNotSupportedException(signature));
    }
}

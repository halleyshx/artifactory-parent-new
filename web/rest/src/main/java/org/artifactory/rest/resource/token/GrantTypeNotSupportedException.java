package org.artifactory.rest.resource.token;

import javax.annotation.Nonnull;

/**
 * @author Yinon Avraham.
 */
public class GrantTypeNotSupportedException extends TokenRequestException {

    public GrantTypeNotSupportedException(@Nonnull String grantTypeSignature) {
        super(TokenResponseErrorCode.UnsupportedGrantType, "Grant type is not supported: " + grantTypeSignature);
    }
}

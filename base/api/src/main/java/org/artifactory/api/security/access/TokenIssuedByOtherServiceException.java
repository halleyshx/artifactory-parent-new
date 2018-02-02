package org.artifactory.api.security.access;

import org.jfrog.access.common.ServiceId;

/**
 * Created by Yinon Avraham.
 */
public class TokenIssuedByOtherServiceException extends RuntimeException {

    private final ServiceId currentServiceId;
    private final ServiceId otherServiceId;

    public TokenIssuedByOtherServiceException(String message, ServiceId currentServiceId, ServiceId otherServiceId) {
        super(message);
        this.currentServiceId = currentServiceId;
        this.otherServiceId = otherServiceId;
    }

    public ServiceId getCurrentServiceId() {
        return currentServiceId;
    }

    public ServiceId getOtherServiceId() {
        return otherServiceId;
    }
}

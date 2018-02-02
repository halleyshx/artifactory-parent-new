package org.artifactory.security.access;

import org.jfrog.access.common.ServiceId;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

import static org.artifactory.security.access.AccessServiceConstants.ARTIFACTORY_SERVICE_ID_REGEX;

/**
 * @author Yinon Avraham.
 */
public class ArtifactoryAdminScopeToken {

    private static final String ADMIN_SUFFIX = ":admin";
    static final Pattern SCOPE_ARTIFACTORY_ADMIN_PATTERN = Pattern.compile(ARTIFACTORY_SERVICE_ID_REGEX + ADMIN_SUFFIX);

    /**
     * Check whether a scope token is a valid artifactory admin scope token
     * @param scopeToken the scope token to parse
     */
    public static boolean accepts(String scopeToken) {
        return scopeToken != null && SCOPE_ARTIFACTORY_ADMIN_PATTERN.matcher(scopeToken).matches();
    }

    public static ArtifactoryAdminScopeToken parse(String scopeToken) {
        if (!accepts(scopeToken)) {
            throw new IllegalArgumentException("Not a valid artifactory admin scope token:" + scopeToken);
        }
        String serviceIdName = scopeToken.substring(0, scopeToken.length() - ADMIN_SUFFIX.length());
        return new ArtifactoryAdminScopeToken(ServiceId.fromFormattedName(serviceIdName));
    }

    private final ServiceId serviceId;

    public ArtifactoryAdminScopeToken(ServiceId serviceId) {
        this.serviceId = serviceId;
    }

    @Nonnull
    public ServiceId getServiceId() {
        return serviceId;
    }

    /**
     * Get the formatted scope token
     */
    @Nonnull
    public String getScopeToken() {
        return serviceId.getFormattedName() + ADMIN_SUFFIX;
    }

    @Override
    public String toString() {
        return getScopeToken();
    }
}

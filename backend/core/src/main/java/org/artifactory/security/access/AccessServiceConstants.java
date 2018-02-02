package org.artifactory.security.access;

import org.jfrog.access.common.ServiceType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.join;
import static org.jfrog.access.common.ServiceId.ELEMENT_PATTERN;
import static org.jfrog.access.common.ServiceId.createServiceIdRegex;

/**
 * @author Yinon Avraham.
 */
public abstract class AccessServiceConstants {

    private AccessServiceConstants() {
        //utility class
    }

    /**
     * The Artifactory service type, used as part of the service ID.
     */
    public static final String ARTIFACTORY_SERVICE_TYPE = ServiceType.ARTIFACTORY;

    /**
     * All Artifactory service types, including the currently effective name and any deprecated names.
     */
    private static final List<String> ARTIFACTORY_SERVICE_TYPES = Arrays.asList(ARTIFACTORY_SERVICE_TYPE, "jf-artifactory");

    private static final String ARTIFACTORY_SERVICE_TYPES_REGEX = "(" + join("|", ARTIFACTORY_SERVICE_TYPES) + ")";
    private static final String ANY_INSTANCE_ID_REGEX = "(" + ELEMENT_PATTERN + "|\\*)";

    /**
     * A regular expression of all artifactory service types - current and deprecated (for backward compatibility)
     */
    public static final String ARTIFACTORY_SERVICE_ID_REGEX = createServiceIdRegex(ARTIFACTORY_SERVICE_TYPES_REGEX);

    /**
     * A pattern that matches a service ID with either of the artifactory service type (current or deprecated) and any
     * instance ID - specific or any (specified by "*")
     */
    public static final Pattern ARTIFACTORY_SERVICE_ANY_ID_PATTERN = Pattern.compile(createServiceIdRegex(
            ARTIFACTORY_SERVICE_TYPES_REGEX, ANY_INSTANCE_ID_REGEX));
}

package org.artifactory.api.security.access;

import org.jfrog.access.common.ServiceId;
import org.jfrog.access.common.SubjectFQN;

import javax.annotation.Nonnull;

/**
 * @author Yinon Avraham.
 */
public class GenericTokenSpec extends TokenSpec<GenericTokenSpec> {

    private final SubjectFQN subject;

    private GenericTokenSpec(@Nonnull String subject) {
        this.subject = SubjectFQN.fromFullyQualifiedName(subject);
    }

    /**
     * Create a new generic token specification.
     * @param subject the subject
     * @return a new empty generic token specification
     */
    @Nonnull
    public static GenericTokenSpec create(@Nonnull String subject) {
        return new GenericTokenSpec(subject);
    }

    @Override
    public SubjectFQN createSubject(ServiceId serviceId) {
        return subject;
    }

    public static boolean accepts(String subject) {
        try {
            SubjectFQN.fromFullyQualifiedName(subject);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

package org.artifactory.ui.rest.service.onboarding;

import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author nadavy
 */
public abstract class OnboardingServiceFactory {

    @Lookup
    public abstract GetArtifactoryInitStatusService getArtifactoryInitStatusService();

    @Lookup
    public abstract GetUnsetReposService getUnsetReposService();

    @Lookup
    public abstract CreateDefaultReposService createDefaultReposService();
}

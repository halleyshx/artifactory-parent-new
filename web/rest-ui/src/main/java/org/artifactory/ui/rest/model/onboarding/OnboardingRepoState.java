package org.artifactory.ui.rest.model.onboarding;

/**
 * @author nadavy
 */
public enum OnboardingRepoState {
    ALREADY_SET(),
    UNSET(),
    UNAVAILABLE();

    OnboardingRepoState() {
    }
}

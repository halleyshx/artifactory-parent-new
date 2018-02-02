package org.artifactory.ui.rest.model.onboarding;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.model.BaseModel;

import java.util.Map;

/**
 * List of repo types with their current state in artifactory -
 * Already exist, not set or unavailable (in oss)
 *
 * @author nadavy
 */
public class OnboardingReposStateModel extends BaseModel {

    public OnboardingReposStateModel() {
    }

    private Map<RepoType, OnboardingRepoState> repoStates;

    public OnboardingReposStateModel(Map<RepoType, OnboardingRepoState> repoStates) {
        this.repoStates = repoStates;
    }

    public Map<RepoType, OnboardingRepoState> getRepoStates() {
        return repoStates;
    }
}

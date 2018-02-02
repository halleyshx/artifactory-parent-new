package org.artifactory.ui.rest.service.onboarding;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.onboarding.OnboardingRepoState;
import org.artifactory.ui.rest.model.onboarding.OnboardingReposStateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.artifactory.repo.config.RepoConfigDefaultValues.EXAMPLE_REPO_KEY;

/**
 * Return a map of repo types to one of 3 possible states - unset, already set or unavailable (in oss)
 *
 * @author nadavy
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetUnsetReposService implements RestService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private AddonsManager addonsManager;

    private final static List<RepoType> unsupportedRepoTypes = Lists.newArrayList(
            RepoType.P2,
            RepoType.VCS,
            RepoType.Distribution
    );

    private final static List<RepoType> ossSupportedTypes = Lists.newArrayList(
            RepoType.Maven,
            RepoType.Gradle,
            RepoType.Ivy,
            RepoType.SBT,
            RepoType.Generic
    );

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        Map<RepoType, OnboardingRepoState> repoTypesMap = getSupportedRepoTypesMap();
        filterRepoTypesMap(repoTypesMap);
        response.iModel(new OnboardingReposStateModel(repoTypesMap));
    }

    /**
     * Filter repositories already set up
     */
    private void filterRepoTypesMap(Map<RepoType, OnboardingRepoState> repoTypesMap) {
        repositoryService.getLocalRepoDescriptors().stream()
                .filter(repoDescriptor -> !repoDescriptor.getKey().equals(EXAMPLE_REPO_KEY))
                .forEach(repoDescriptor -> repoTypesMap.replace(repoDescriptor.getType(),
                        OnboardingRepoState.ALREADY_SET));
        repositoryService.getRemoteRepoDescriptors().stream()
                .filter(repoDescriptor -> !repoDescriptor.getKey().equals("jcenter"))
                .forEach(repoDescriptor -> repoTypesMap.replace(repoDescriptor.getType(),
                        OnboardingRepoState.ALREADY_SET));
        repositoryService.getVirtualRepoDescriptors()
                .forEach(repoDescriptor -> repoTypesMap.replace(repoDescriptor.getType(),
                        OnboardingRepoState.ALREADY_SET));
    }

    /**
     * Return a map of supported repo types for onboarding setup
     */
    private Map<RepoType, OnboardingRepoState> getSupportedRepoTypesMap() {
        Map<RepoType, OnboardingRepoState> repoTypesMap = Maps.newHashMap();
        boolean isOss = addonsManager instanceof OssAddonsManager;
        OnboardingRepoState defaultState = isOss ? OnboardingRepoState.UNAVAILABLE :
                OnboardingRepoState.UNSET;
        // set all repo types to the default state
        Arrays.stream(RepoType.values())
                .forEach(repoType -> repoTypesMap.put(repoType, defaultState));
        // in oss make only ossSupportedTypes available
        if (isOss) {
            ossSupportedTypes.forEach(repoType -> repoTypesMap.put(repoType, OnboardingRepoState.UNSET));
        }
        // remove unsupported repo types
        unsupportedRepoTypes.forEach(repoTypesMap::remove);
        return repoTypesMap;
    }
}

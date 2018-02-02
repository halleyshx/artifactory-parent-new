package org.artifactory.ui.rest.service.onboarding;

import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.ConstantValues;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.UserInfo;
import org.artifactory.ui.rest.model.onboarding.ArtifactoryInitStatusModel;
import org.jfrog.security.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.artifactory.repo.config.RepoConfigDefaultValues.EXAMPLE_REPO_KEY;

/**
 * @author nadavy
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetArtifactoryInitStatusService implements RestService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        RepoPath repoPath = RepoPathFactory.create(EXAMPLE_REPO_KEY, "");
        // has default repo, it's empty, and no other repos exists
        boolean hasOnlyEmptyDefaultRepo = repositoryService.exists(repoPath) &&
                repositoryService.getArtifactCount(repoPath) == 0 &&
                repositoryService.getAllRepoKeys().size() == 1;
        boolean hasLicenseAlready = addonsManager.isLicenseInstalled();

        long lastLoginTime;
        UserInfo userInfo = userGroupService.currentUser();
        if (userInfo != null && userInfo.getUsername().equals("admin")) {
            lastLoginTime = userInfo.getLastLoginTimeMillis();
        } else {
            lastLoginTime = Optional.ofNullable(securityService.getUserLastLoginInfo("admin"))
                    .map(Pair::getSecond)
                    .orElse(0L);
        }

        boolean hasPriorLogins = lastLoginTime != 0;

        boolean skipWizard = ConstantValues.skipOnboardingWizard.getBoolean();
        boolean hasProxies = !configService.getDescriptor().getProxies().isEmpty();
        ArtifactoryInitStatusModel artifactoryInitStatusModel = new ArtifactoryInitStatusModel(!hasOnlyEmptyDefaultRepo,
                hasLicenseAlready, hasPriorLogins, hasProxies, skipWizard);
        response.iModel(artifactoryInitStatusModel);
    }
}

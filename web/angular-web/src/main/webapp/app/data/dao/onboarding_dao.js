import {ArtifactoryDao} from '../artifactory_dao';

export function OnboardingDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setPath(RESOURCE.ONBOARDING + "/:param")
        .setCustomActions({
            'initStatus': {
                method: 'GET',
                params: {param: 'initStatus'}
            },
            'reposStates': {
                method: 'GET',
                params: {param: 'reposStates'}
            },
            'createDefaultRepos': {
                method: 'POST',
                params: {param: 'createDefaultRepos'}
            }
        })
        .getInstance();
}
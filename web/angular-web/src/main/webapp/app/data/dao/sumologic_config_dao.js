export function SumoLogicConfigDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setPath(RESOURCE.SUMOLOGIC + '/:action')
        .setCustomActions({
            'registerSumoLogicApplication': {
                method: 'POST',
                params: {action: 'registerSumoLogicApplication'},
                notifications: true
            },
            'setupSumoLogicApplication': {
                method: 'POST',
                params: {action: 'setupSumoLogicApplication'},
                notifications: true
            },
            'refreshToken': {
                method: 'POST',
                params: {action: 'refreshToken'},
                notifications: true
            },
            'reset': {
                method: 'POST',
                params: {action: 'reset'},
                notifications: true
            }
        })
        .getInstance();
}
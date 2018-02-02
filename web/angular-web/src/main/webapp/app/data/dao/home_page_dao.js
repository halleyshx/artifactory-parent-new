export function HomePageDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setCustomActions({
            'get': {
                method: 'GET',
                params: {$no_spinner: true}
            }
        })
        .setPath(RESOURCE.HOME_PAGE + '/widget/:widgetName')
        .getInstance();
}
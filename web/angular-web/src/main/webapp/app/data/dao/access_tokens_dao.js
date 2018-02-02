export function AccessTokensDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.ACCESSTOKENS + '/:action')
            .setCustomActions({
                'getTokens': {
                    method: 'GET',
                    params: {action: 'tokens'},
                    isArray: true
                },
                'revokeTokens': {
                    method: 'POST',
                    notifications: true,
                    params: {action: 'revokeTokens'}
                }
            })
            .getInstance();
}
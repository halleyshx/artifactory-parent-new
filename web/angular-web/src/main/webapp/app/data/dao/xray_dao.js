export function XrayDao(RESOURCE, ArtifactoryDaoFactory) {
	return ArtifactoryDaoFactory()
			.setPath(RESOURCE.XRAY + '/:action')
			.setCustomActions({
				'getNoneIndex': {
					method: 'GET',
					params: {action: 'getNoneIndex'},
					isArray: true
				},
				'getIndex': {
					method: 'GET',
					params: {action: 'getIndex'},
					isArray: true
				},
				'addIndex': {
					method: 'POST',
					params: {action: 'addIndex'}
				},
				'removeIndex': {
					method: 'POST',
					params: {action: 'removeIndex'}
				},
				'updateRepositories': {
					method: 'PUT',
					params: {action: 'indexRepos'}
				},
				'getConf': {
					path: RESOURCE.XRAY_CONFIG,
					method: 'GET'
				},
				'isXrayEnabled': {
					method: 'GET',
					params: {action: 'isXrayEnabled'}
				},
				'setXrayEnabled': {
					method: 'POST',
					params: {action: 'setXrayEnabled'}
				}
			})
			.getInstance();
}
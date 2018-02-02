import {ArtifactoryDao} from "../artifactory_dao";

export class GroupsDao extends ArtifactoryDao {

    constructor($resource, RESOURCE, artifactoryNotificationsInterceptor) {
        super($resource, RESOURCE,artifactoryNotificationsInterceptor);
        this.setUrl(RESOURCE.API_URL + RESOURCE.GROUPS + '/:prefix/:name');

        this.setCustomActions({
            'getAll': {
                method: 'GET',
                isArray: true
            },
            'getSingle': {
                method: 'GET',
                params: {name: '@name'},
                notifications: true
            },
            'update': {
                method: 'PUT',
                params: {name: '@name'},
                notifications: true
            },
            'create': {
                method: 'POST',
                notifications: true
            },
            'delete': {
                method: 'POST',
                params: {prefix: 'delete'},
                notifications: true
            }
        });
    }
}


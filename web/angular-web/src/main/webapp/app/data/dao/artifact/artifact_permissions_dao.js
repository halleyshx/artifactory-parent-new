import {ArtifactoryDao} from '../../artifactory_dao';

export class ArtifactPermissionsDao extends ArtifactoryDao {

    constructor($resource, RESOURCE, artifactoryNotificationsInterceptor) {
        super($resource,RESOURCE, artifactoryNotificationsInterceptor);

        this.setUrl(RESOURCE.API_URL + RESOURCE.ARTIFACT_PERMISSIONS);
        this.setCustomActions({
            query: {
                isArray: false
            }
        })
    }
}

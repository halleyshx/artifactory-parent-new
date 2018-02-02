import {ArtifactoryDao} from '../../artifactory_dao';

export class ArtifactViewSourceDao extends ArtifactoryDao {

    constructor($resource, RESOURCE, artifactoryNotificationsInterceptor) {
        super($resource,RESOURCE, artifactoryNotificationsInterceptor);

        this.setUrl(RESOURCE.API_URL + RESOURCE.ARTIFACT_VIEW_SOURCE);
    }
}

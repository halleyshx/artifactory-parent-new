import {ArtifactoryDao} from '../artifactory_dao';

export function SamlDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.SAML_CONFIG)
}



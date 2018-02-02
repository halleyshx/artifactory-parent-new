/**
 * Created by tomere on 08/06/2017.
 */

import {ArtifactoryDao} from '../artifactory_dao';

export function SslCertificateDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setDefaults({method: 'POST'})
            .setPath(RESOURCE.SSLCERTIFICATES + "/:action")
            .setCustomActions({
                'add': {
                    method: 'POST',
                    notifications: true,
                    isArray: true,
                    params: {action: 'add'},
                },
                'delete': {
                    method: 'POST',
                    notifications: true,
                    //isArray: true,
                    params: {action: 'delete'}
                },
                'getDetails': {
                    method: 'GET',
                    notifications: true,
                    isArray: false,
                    params: {action: 'details'},
                },
                'getAllCertificates': {
                    method: 'GET',
                    notifications: true,
                    isArray: true,
                    params: {action: 'getAllCertificates'},
                },
            })
            .getInstance();
}
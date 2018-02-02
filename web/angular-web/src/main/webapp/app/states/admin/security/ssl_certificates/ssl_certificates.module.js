/**
 * Created by tomere on 07/06/2017.
 */
import {SslCertificatesController} from './ssl_certificates.controller';

function sslCertificatesConfig($stateProvider) {

    $stateProvider
            .state('admin.security.ssl_certificates', {
                params: {feature: 'sslcertificates'},
                url: '/ssl_certificates',
                templateUrl: 'states/admin/security/ssl_certificates/ssl_certificates.html',
                controller: 'SslCertificatesController as SslCertificates'
            });
}

export default angular.module('security.ssl_certificates', [])
        .config(sslCertificatesConfig)
        .controller('SslCertificatesController', SslCertificatesController);
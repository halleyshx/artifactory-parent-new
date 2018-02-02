import {AccessTokensController} from './access_tokens.controller';

function accessTokensConfig($stateProvider) {

    $stateProvider
            .state('admin.security.access_tokens', {
                params: {feature: 'accesstokens'},
                url: '/access_tokens',
                templateUrl: 'states/admin/security/access_tokens/access_tokens.html',
                controller: 'AccessTokensController as AccessTokens'
            })
}

export default angular.module('security.access_tokens', [])
        .config(accessTokensConfig)
        .controller('AccessTokensController', AccessTokensController);
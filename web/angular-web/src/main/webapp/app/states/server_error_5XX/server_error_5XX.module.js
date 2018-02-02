/**
 * Created by tomere on 16/03/2017.
 */

function config ($stateProvider) {

    $stateProvider
            .state('server_error_5XX', {
                templateUrl: 'states/server_error_5XX/server_error_5XX.html',
                parent: 'app-layout',
            })
}

export default angular.module('server_error_5XX', [])
        .config(config)
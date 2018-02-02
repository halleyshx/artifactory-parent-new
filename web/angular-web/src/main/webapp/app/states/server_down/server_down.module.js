function config ($stateProvider) {

    $stateProvider
            .state('server_down', {
                templateUrl: 'states/server_down/server_down.html'
            })
}

export default angular.module('server_down', [])
        .config(config)
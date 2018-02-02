import {AdminConfigurationXrayController} from './xray.controller';

function xrayConfig($stateProvider) {
    $stateProvider
            .state('admin.configuration.xray', {
                params: {feature: 'xray'},
                url: '/xray',
                templateUrl: 'states/admin/configuration/xray/xray.html',
                controller: 'AdminConfigurationXrayController as AdminConfigurationXray'
            })
}

export default angular.module('configuration.xray', [])
        .config(xrayConfig)
        .controller('AdminConfigurationXrayController', AdminConfigurationXrayController)
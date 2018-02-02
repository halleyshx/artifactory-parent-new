import {AdminAdvancedLogAnalyticsController} from "./log_analytics.controller";

function logAnalyticsConfig($stateProvider) {
    $stateProvider
            .state('admin.advanced.log_analytics', {
                url: '/log_analytics',
                templateUrl: 'states/admin/advanced/log_analytics/log_analytics.html',
                controller: 'AdminAdvancedLogAnalyticsController as LogAnalytics'
            })
}

export default angular.module('advanced.log_analytics', [])
        .config(logAnalyticsConfig)
        .controller('AdminAdvancedLogAnalyticsController', AdminAdvancedLogAnalyticsController);
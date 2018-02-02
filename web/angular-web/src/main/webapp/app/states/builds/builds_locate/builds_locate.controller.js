import EVENTS from '../../../constants/artifacts_events.constants';

export class BuildsLocateController {
    constructor($scope, $timeout, $location, $stateParams, BuildsDao) {
        this.$scope = $scope;
        this.$stateParams = $stateParams;
        this.$timeout = $timeout;
        this.$location = $location;
        this.buildsDao = BuildsDao;
        this._getBuildsData();
    }

    _getBuildsData() {
        return this.buildsDao.lastBuild({
            name: this.$stateParams.buildName,
            number: this.$stateParams.buildNumber
        }).$promise.then((data) => {
            //
            this.$location.path('/builds/'+this.$stateParams.buildName+'/'+this.$stateParams.buildNumber+'/'+data.time+'/general/')
        });
    }
}
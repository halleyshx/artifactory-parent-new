import DICTIONARY from "./../../constants/builds.constants";

class jfGeneralInfoController {
    constructor($scope, $state, $stateParams, BuildsDao, PushToBintrayModal, User, DistributionDao, JFrogModal) {
        this.$scope = $scope;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.buildsDao = BuildsDao;
        this.pushToBintrayModal = PushToBintrayModal;
        this.generalData = {};
        this.distributionDao = DistributionDao;
        this.DICTIONARY = DICTIONARY.generalInfo;
        this.User = User;
        this.modal = JFrogModal;

        //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
        // this.userCanPushToBintray = false;
        this.userCanDistribute = false;

        this._getGeneralInfo();
    }

    pushToBintray() {
        this.modalInstance = this.pushToBintrayModal.launchModal('build');
    }

    distribute() {
        this.distributionDao.getAvailableDistributionRepos({}).$promise.then((data)=>{
            let modalInstance;
            this.distributeModalScope = this.$scope.$new();

            this.distributeModalScope.title = "Distribute " + this.$stateParams.buildName + " #" + this.$stateParams.buildNumber;
            this.distributeModalScope.distributionRepositoriesOptions = _.map(data.data.availableDistributionRepos, 'repoKey');

            this.distributeModalScope.data = {};
            this.distributeModalScope.data.async = true;
            this.distributeModalScope.data.publish = true;
            this.distributeModalScope.data.publish = true;
            this.distributeModalScope.data.overrideExistingFiles = false;
            this.distributeModalScope.data.selectedRepo = null;
            this.distributeModalScope.distType = "build";
            this.distributeModalScope.distribute = () => {
                this._resetChanges();
                this.distributionDao.distributeBuild({
                    targetRepo: this.distributeModalScope.data.selectedRepo,
                    async: this.distributeModalScope.data.async,
                    overrideExistingFiles: this.distributeModalScope.data.overrideExistingFiles,
                    date: this.$stateParams.startTime
                },{
                    buildName: this.$stateParams.buildName,
                    buildNumber: this.$stateParams.buildNumber,
                    date: this.$stateParams.startTime
                }).$promise.then((res)=>{
                    // Success
                    if (this.distributeModalScope.data.async) {
                        modalInstance.close();
                    } else {
                        this._runRulesTest(res);
                    }
                });
            };

            // DRY RUN
            this.distributeModalScope.dryRun = () => {
                this._resetChanges();
                this.distributionDao.distributeBuild({
                    targetRepo: this.distributeModalScope.data.selectedRepo,
                    async: this.distributeModalScope.data.async,
                    publish: this.distributeModalScope.data.publish,
                    overrideExistingFiles: this.distributeModalScope.data.overrideExistingFiles,
                    dryRun: true
                }, {
                        buildName: this.$stateParams.buildName,
                        buildNumber: this.$stateParams.buildNumber,
                        date: this.$stateParams.startTime
                }
                ).$promise.then((res)=>{
                    this._runRulesTest(res);
                });
            };

            modalInstance = this.modal.launchModal('distribute_modal', this.distributeModalScope, 650);
        });
    }
    _runRulesTest(res) {
        let ind = 0;
        let result = res.data;
        _.forEach(result, (value,key) => {
            if (key == 'distributed') {
                let distributed = result[key];

                _.forEach(distributed, (value,key) => {
                    distributed[key].customId = "dis" + ind;
                    ind++;

                    let packages = distributed[key].packages;

                    _.forEach(packages, (value,key) => {
                        packages[key].customId = "pac" + ind;
                        ind++;

                        let versions = packages[key].versions;
                        _.forEach(versions, (value,key) => {
                            versions[key].customId = "ver" + ind;
                            ind++;
                        });

                    });
                });
            }
        });
        this.distributeModalScope.data.dryRunResults = result;

        _.forEach(result.messagesByPath, (value) => {
            if (value.warnings) {
                this.distributeModalScope.data.warningExist = value.warnings.length ? true : false;
            }
            if (value.errors) {
                this.distributeModalScope.data.errorsExist = value.errors.length ? true : false;
            }
        });
    }
    _resetChanges() {
        // RESET
        this.distributeModalScope.data.dryRunResults = null;
        this.distributeModalScope.data.toggleSuccessTitle = null;
        this.distributeModalScope.data.toggleWarnTitle = null;
        this.distributeModalScope.data.toggleErrorTitle = null;
        this.distributeModalScope.data.warningExist = null;
        this.distributeModalScope.data.errorsExist = null;
    }
    _getGeneralInfo() {
        return this.buildsDao.getData({
            name: this.$stateParams.buildName,
            number: this.$stateParams.buildNumber,
            time: this.$stateParams.startTime,
            action:'buildInfo'
        }).$promise.then((data) => {
            //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
            // this.userCanPushToBintray = data.allowPushToBintray && this.User.getCurrent().canPushToBintray();
            this.userCanDistribute = data.userCanDistribute;
            this.$stateParams.startTime = data.time;
            this.$state.transitionTo('.', this.$stateParams, { location: 'replace', inherit: true, relative: this.$state.$current, notify: false })
            this.generalData = data;
        });

    }

}


export function jfGeneralInfo() {
    return {
        restrict: 'EA',
        controller: jfGeneralInfoController,
        controllerAs: 'jfGeneralInfo',
        scope: {},
        bindToController: true,
        templateUrl: 'states/builds/builds_info/build_tabs/jf_general_info.html'
    }
}
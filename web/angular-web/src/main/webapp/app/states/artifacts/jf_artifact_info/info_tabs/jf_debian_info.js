import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';

class jfDebianInfoController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {

        this.artifactoryGridFactory = JFrogGridFactory;
        this.artifactViewsDao = ArtifactViewsDao;
        this.JFrogEventBus = JFrogEventBus;
        this.DICTIONARY = DICTIONARY.debian;
        this.debianData = {};
        this.debianDependenciesGridOptions = {};
        this.$scope = $scope;

        this.initDebianInfo();
    }

    initDebianInfo() {
        this._registerEvents();
        this._getDebianInfoData();
    }

    _getDebianInfoData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "debian",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
                .then((data) => {
                    this.debianData = data;
                    this._createGrid();
                });
    }

    _createGrid() {


        this.formattedDependencies = [];
        _.forEach(this.debianData.debianDependencies, (item) => {
            this.formattedDependencies.push({
                name: item
            })
        });

        if (this.debianData.debianDependencies) {
            this.debianDependenciesGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setRowTemplate('default')
                    .setColumns(this._getColumns('dependencies'))
                    .setGridData(this.formattedDependencies)
        }
    }

    _getColumns(gridType) {
        if (gridType === 'dependencies') {
            return [
                {
                    name: 'Name',
                    displayName: 'Name',
                    field: 'name'
                }]
        }
    }

    _registerEvents() {
        let self = this;
        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            if (this.currentNode != node) {
                this.currentNode = node;
                self._getDebianInfoData();
            }
        });
    }
}

export function jfDebianInfo() {
    return {
        restrict: 'EA',
        controller: jfDebianInfoController,
        controllerAs: 'jfDebianInfo',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_debian_info.html'
    }
}
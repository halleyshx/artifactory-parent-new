import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';

class jfOpkgInfoController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {

        this.artifactoryGridFactory = JFrogGridFactory;
        this.artifactViewsDao = ArtifactViewsDao;
        this.JFrogEventBus = JFrogEventBus;
        this.DICTIONARY = DICTIONARY.opkg;
        this.opkgData = {};
        this.opkgDependenciesGridOptions = {};
        this.$scope = $scope;

        this.initOpkgInfo();
    }

    initOpkgInfo() {
        this._registerEvents();
        this._getOpkgInfoData();
    }

    _getOpkgInfoData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "opkg",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
                .then((data) => {
                    this.opkgData = data;
                    this._createGrid();
                });
    }

    _createGrid() {


        this.formattedDependencies = [];
        _.forEach(this.opkgData.debianDependencies, (item) => {
            this.formattedDependencies.push({
                name: item
            })
        });

        if (this.opkgData.debianDependencies) {
            this.opkgDependenciesGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
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
                self._getOpkgInfoData();
            }
        });
    }
}

export function jfOpkgInfo() {
    return {
        restrict: 'EA',
        controller: jfOpkgInfoController,
        controllerAs: 'jfOpkgInfo',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_opkg_info.html'
    }
}
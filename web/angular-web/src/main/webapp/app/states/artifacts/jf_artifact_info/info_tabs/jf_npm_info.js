import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';
class jfNpmInfoController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {
        this.artifactoryGridFactory = JFrogGridFactory;
        this.artifactViewsDao = ArtifactViewsDao;
        this.JFrogEventBus = JFrogEventBus;
        this.DICTIONARY = DICTIONARY.npm;
        this.npmData = {};
        this.npmDependenciesGridOptions = {};
        this.$scope = $scope;

        this._initNpmInfo();
    }

    _initNpmInfo() {
        this._getNpmInfoData();
        this._registerEvents();
    }

    _getNpmInfoData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "npm",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
            .then((data) => {
                this.npmData = data;
                this._createGrid();
            })
    }

    _createGrid() {
        if (this.npmData.npmDependencies) {
            if (!Object.keys(this.npmDependenciesGridOptions).length) {
                this.npmDependenciesGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setRowTemplate('default')
                    .setColumns(this._getColumns())
                    .setGridData(this.npmData.npmDependencies);
            } else {
                this.npmDependenciesGridOptions.setGridData(this.npmData.npmDependencies)
            }
        }
    }

    _getColumns() {
        return [
            {
                name: 'name',
                displayName: 'Name',
                field: 'name'
            },
            {
                name: 'Version',
                displayName: 'Version',
                field: 'version'
            }]
    }

    _registerEvents() {
        let self = this;
        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            if (this.currentNode != node) {
                this.currentNode = node;
                self._getNpmInfoData();
            }
        });
    }
}

export function jfNpmInfo() {
    return {
        restrict: 'EA',
        controller: jfNpmInfoController,
        controllerAs: 'jfNpmInfo',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_npm_info.html'
    }
}
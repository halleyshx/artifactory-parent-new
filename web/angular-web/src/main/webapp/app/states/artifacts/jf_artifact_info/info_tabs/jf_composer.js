import EVENTS from "../../../../constants/artifacts_events.constants";
import DICTIONARY from "./../../constants/artifact_general.constant";
class jfComposerController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {
        this.artifactViewsDao = ArtifactViewsDao;
        this.artifactoryGridFactory = JFrogGridFactory;
        this.JFrogEventBus = JFrogEventBus;
        this.DICTIONARY = DICTIONARY.composer;
        this.gridDependenciesOptions = {};
        this.composerData = {};
        this.$scope = $scope;
        this._initComposer();
    }

    _initComposer() {
        this._registerEvents();
        this.getComposerData();
    }

    getComposerData() {

        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "composer",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
                .then((data) => {
                    this.composerData = data;
                    this._createGrid();
                });
    }

    _createGrid() {
        if (this.composerData.composerDependencies) {
            if (!Object.keys(this.gridDependenciesOptions).length) {
                this.gridDependenciesOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                        .setRowTemplate('default')
                        .setColumns(this._getColumns('dependencies'))
                        .setGridData(this.composerData.composerDependencies)
            } else {
                this.gridDependenciesOptions.setGridData(this.composerData.composerDependencies)
            }
        }
    }

    _getColumns(gridType) {
        if (gridType === 'dependencies') {
            return [
                {
                    name: 'Name',
                    displayName: 'Name',
                    field: 'name'
                },
                {
                    name: 'Version',
                    displayName: 'Version',
                    field: 'version'
                }]
        }
    }
    _registerEvents() {
        let self = this;

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            if (this.currentNode != node) {
                this.currentNode = node;
                self.getComposerData();
            }
        });
    }

}
export function jfComposer() {
    return {
        restrict: 'EA',
        controller: jfComposerController,
        controllerAs: 'jfComposer',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_composer.html'
    }
}
import EVENTS from "../../../../constants/artifacts_events.constants";
import DICTIONARY from "./../../constants/artifact_general.constant";
class jfPuppetController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {
        this.artifactoryGridFactory = JFrogGridFactory;
        this.artifactViewsDao = ArtifactViewsDao;
        this.JFrogEventBus = JFrogEventBus;
        this.DICTIONARY = DICTIONARY.puppet;
        this.puppetData = {};
        this.puppetKeywordsGridOptions = {};
        this.puppetDependenciesGridOptions = {};
        this.$scope = $scope;

        this._initPuppetInfo();
    }

    _initPuppetInfo() {
        this._getPuppetInfoData();
        this._registerEvents();
    }

    _getPuppetInfoData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "puppet",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
                .then((data) => {
                    this.puppetData = data;
                    this._createGrids();
                })
    }

    _createGrids() {
        if (this.puppetData.puppetKeywords) {
            if (!Object.keys(this.puppetKeywordsGridOptions).length) {
                this.puppetKeywordsGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                        .setRowTemplate('default')
                        .setColumns(this._getKeywordsColumns())
                        .setGridData(this.puppetData.puppetKeywords);
            } else {
                this.puppetKeywordsGridOptions.setGridData(this.puppetData.puppetKeywords)
            }
        }
        if (this.puppetData.puppetDependencies) {
            if (!Object.keys(this.puppetDependenciesGridOptions).length) {
                this.puppetDependenciesGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                        .setRowTemplate('default')
                        .setColumns(this._getDependenciesColumns())
                        .setGridData(this.puppetData.puppetDependencies);
            } else {
                this.puppetDependenciesGridOptions.setGridData(this.puppetData.puppetDependencies)
            }
        }
    }

    _getKeywordsColumns() {
        return [
            {
                name: 'name',
                displayName: 'Name',
                field: 'name'
            }]
    }

    _getDependenciesColumns() {
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
                self._getPuppetInfoData();
            }
        });
    }
}

export function jfPuppet() {
    return {
        restrict: 'EA',
        controller: jfPuppetController,
        controllerAs: 'jfPuppet',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_puppet.html'
    }
}
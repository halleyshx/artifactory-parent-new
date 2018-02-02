/**
 * Created by matang on 09/11/2017.
 */

import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';

class jfHelmController {
    constructor($scope, $element, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {
        this.$scope = $scope;
        this.$element = $element;
        this.artifactViewsDao = ArtifactViewsDao;
        this.DICTIONARY = DICTIONARY.helm;
        this.gridDependenciesOptions = {};
        this.JFrogEventBus = JFrogEventBus;
        this.artifactoryGridFactory = JFrogGridFactory;
        this.helmData = {};

        this._getHelmData();
        this._registerEvents();
    }


    _getHelmData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "helm",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise.then((data) => {
            this.helmData = data;
            this._createGrid();
        });
    }

    _createGrid() {
        if (this.helmData.helmDependencies) {
            this.gridDependenciesOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setRowTemplate('default')
                    .setColumns(this._getColumns())
                    .setGridData(this.helmData.helmDependencies)
        }
    }

    _getColumns() {
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


    _registerEvents() {
        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            this.currentNode = node;
            this._getHelmData();
        });
    }
}
export function jfHelm() {
    return {
        restrict: 'EA',
        controller: jfHelmController,
        controllerAs: 'jfHelm',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_helm.html'
    }
}
import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';

class jfConanController {
    constructor($scope, $element, ArtifactViewsDao, JFrogEventBus, JFrogGridFactory) {
        this.$scope = $scope;
        this.$element = $element;
        this.artifactViewsDao = ArtifactViewsDao;
        this.DICTIONARY = DICTIONARY.conan;
        this.JFrogEventBus = JFrogEventBus;
        this.artifactoryGridFactory = JFrogGridFactory;
        this.conanData = {};
        this.labelGridOptions = {};

        this._getConanData();
        this._registerEvents();
    }

    _getConanData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "conan",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise.then((data) => {
            this.conanData = data;
            if (this.layersController)
                this.layersController.refreshView();
        });
    }

    _registerEvents() {
        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            this.currentNode = node;
            this._getConanData();
        });
    }
}

export function jfConan() {
    return {
        restrict: 'EA',
        controller: jfConanController,
        controllerAs: 'jfConan',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_conan.html'
    }
}
import EVENTS from '../../../../constants/artifacts_events.constants';

class jfIvyViewController {
    constructor($scope, ArtifactViewsDao, JFrogEventBus) {
        this.artifactIvyViewDao = ArtifactViewsDao;
        this.JFrogEventBus = JFrogEventBus;
        this.$scope = $scope;
        this._initIvyView();
    }

    _initIvyView() {
        this._registerEvents();
        this._getIvyViewData();
    }

    _getIvyViewData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactIvyViewDao.fetch({
            "view": "pom",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
                .then((data) => {
                    //console.log(data);
                    this.ivyViewData= data;
                    this.ivyViewData.fileContent=data.fileContent.trim();
                })
    }

    _registerEvents() {
        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            if (this.currentNode != node) {
                this.currentNode = node;
                this._getIvyViewData();
            }
        });
    }
}
export function jfIvyView() {
    return {
        restrict: 'EA',
        controller: jfIvyViewController,
        controllerAs: 'jfIvyView',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_ivy_view.html'
    }
}
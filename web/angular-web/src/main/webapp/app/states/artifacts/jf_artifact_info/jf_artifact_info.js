import DICTIONARY from "./../constants/artifact_general.constant";

class jfArtifactInfoController {
    constructor($element, $stateParams, $state, $scope, JFrogEventBus, $timeout, User, ArtifactoryFeatures) {
        this.$element = $element;
        this.stateParams = $stateParams;
        this.state = $state;
        this.features = ArtifactoryFeatures;
        this.$timeout = $timeout;
        this.user = User;
        this.DICTIONARY = DICTIONARY.tabs;
        this.isDropdownOpen = false;
        this.JFrogEventBus = JFrogEventBus;
        this.EVENTS = JFrogEventBus.getEventsDefinition();
        JFrogEventBus.registerOnScope($scope, this.EVENTS.TREE_NODE_SELECT, node => this.selectNode(node)
    )
        ;
        $scope.$on('ui-layout.resize', () => this._refreshTabs());

    }

    selectNode(node) {
        if (node.data) {
            // wait for the element to render and calculate how many tabs should display
            if (!angular.equals(this.infoTabs, node.data.tabs)) {
                this._refreshTabs();
            }
            this.infoTabs = node.data.tabs;
            this._transformInfoTabs();
            this.currentNode = node;
            // if current tab exists in the new node - dispatch an event:
            if (_.findWhere(this.infoTabs, {name: this.stateParams.tab}) && this.stateParams.tab !== 'StashInfo') {
                this.JFrogEventBus.dispatch(this.EVENTS.TAB_NODE_CHANGED, node);
            }
        }
        else {
            this.currentNode = null;
        }
    }

    _refreshTabs() {
        this.JFrogEventBus.dispatch(this.EVENTS.TABS_REFRESH);
    }

    _transformInfoTabs() {
        let features = {
            'Watch': 'watches',
            'Properties': 'properties',
            'Builds': 'builds'
        };
        if (this.infoTabs) this.infoTabs.forEach((tab) => {
            tab.feature = features[tab.name];
        });
    }
}
export function jfArtifactInfo() {
    return {
        restrict: 'E',
        controller: jfArtifactInfoController,
        controllerAs: 'jfArtifactInfo',
        templateUrl: 'states/artifacts/jf_artifact_info/jf_artifact_info.html',
        bindToController: true
    }
}




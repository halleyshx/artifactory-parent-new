import TreeConfig from "./jf_simple_tree.config";
import JFCommonBrowser from "../jf_common_browser/jf_common_browser";

class JfSimpleBrowserController extends JFCommonBrowser {
    constructor($element, $state, $stateParams, $scope, $timeout, $q, TreeBrowserDao, JFrogEventBus, NativeBrowser,
            ArtifactoryState, ArtifactActions, AdvancedStringMatch, JFrogUIUtils) {
        super(ArtifactActions, AdvancedStringMatch, ArtifactoryState);
        this.type="simple";
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.JFrogEventBus = JFrogEventBus;
        this.JFrogUIUtils = JFrogUIUtils;
        this.EVENTS = JFrogEventBus.getEventsDefinition();
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.$element = $element;
        this.$q = $q;
        this.currentNode = {};
        this.treeBrowserDao = TreeBrowserDao;
        this.nativeBrowser = NativeBrowser;
        this.artifactoryState = ArtifactoryState;
        this.treeElement = $(this.$element).find('#tree-element');
        this._registerEvents();

        if (this.browserController) this.browserController.currentBrowser = this;


        let doRefresh = this.artifactoryState.getState('refreshTreeNextTime');
        if (doRefresh) {
            this.treeBrowserDao.invalidateRoots();
            this.artifactoryState.setState('refreshTreeNextTime', false);
        }
    }

    // This is called from link function
    initJSTree() {
        if (_.isEmpty(this.$stateParams.artifact)) {
            // load empty parent (roots)
            this._loadNodeIntoView();
        }
        else {
            // load artifact by path
            this._loadNodeByPath(this.$stateParams.artifact);
        }
    }

    // Preload data for the selected artifact and load it into view
    _loadNodeByPath(path) {
        if (path) {
            this.treeBrowserDao.findNodeByFullPath(path, /* includeArchives = */false)
                    .then((node) => {
                        let transformed = this._transformData([node])[0];
                        if ((! transformed.data.isTrashcan || !transformed.data.isTrashcan()) && !this._shouldShowNode(this.searchText, transformed)) {
                            this.browserController.resetAllFilters();
                            this._loadRootsIntoView();
                        }
                        this._loadNodeIntoView(node);
                    });
        }
        else {
            this._loadNodeIntoView();
        }
    }

    _selectNodeByPath(path) {
        if (path) {
            this.treeBrowserDao.findNodeByFullPath(path, /* includeArchives = */false)
                    .then((node) => {
                        node = this._getNodeByFullPath(node.fullpath);
                        this.jstree().deselect_all();
                        this.jstree().select_node(node);
                    });
        }
        else {
            this._loadNodeIntoView();
        }
    }

    _getNodeByFullPath(path) {
        let treeJson = this.jstree().get_json();
        let node = _.find(treeJson, n => n.data.fullpath === path);
        return node;
    }

    /***************************************************
     * Load the node's data and children (if applicable)
     ***************************************************/
    _loadNodeIntoView(node) {

        if (node === this.treeBrowserDao) {
            this._loadRootsIntoView();
            return;
        }

        if (node) {
            this.selectedNode = node;
        }
        let promise;

        // Not drilling down to repo if didn't click on it
        if (node && (node.parent || this.artifactoryState.getState('tree_touched'))) {
            if (!node.isFolder() && !node.isRepo()) // Not drilling down to files / archives
            {
                this.currentParentNode = node.parent;
            }
            else {
                this.currentParentNode = node;
            }
            promise = this._loadParentIntoView(this.currentParentNode);
        }
        else {
            this.currentParentNode = null;
            promise = this._loadRootsIntoView();
        }
        promise.then(() => this._dispatchEvent());
    }

    _loadParentIntoView(node) {
        // (Adam) Don't use ng-class, it causes major performance issue on large data sets
        return this._loadChildren(node.getChildren()).then(()=>{
            this.treeElement.addClass('has-parent');
        })
    }

    _loadRootsIntoView() {

        if(this.artifactoryState.getState('refreshTreeWhenBackToRoot')){
            this.artifactoryState.setState('refreshTreeWhenBackToRoot',false);
            this._refreshTree();
        }
        // (Adam) Don't use ng-class, it causes major performance issue on large data sets
        return this._loadChildren(this.treeBrowserDao.getRoots()).then(()=>{
            this.treeElement.removeClass('has-parent');
            this._autoSelectFirstIfSelectionNotVisible();
        })
    }

    _loadChildren(promise) {
        return promise.then((children) => {
            // select first child if none selected
            let transformed = this._transformData(children || []);

            let firstVisibleChildIndex = _.findIndex(transformed,child=>{
                return this._shouldShowNode(this.searchText,child);
            });

            if (this.selectedNode && !this._shouldShowNode(this.searchText, this._transformData([this.selectedNode])[0])) {
                this.selectedNode = null;
            }

            this.selectedNode = this.selectedNode || children[firstVisibleChildIndex];

            children = transformed;

            if (this.currentParentNode) {
                // Create a tree with parent and children
                let goUp = {
                    type: 'go_up',
                    data: this.currentParentNode.parent,
                    text: '..'
                };
                let parentTreeNode = this._transformNode(this.currentParentNode);
                parentTreeNode.children = children;
                parentTreeNode.state.opened = true;

                this._buildTree([goUp, parentTreeNode]);
            }
            else {
                // Create a tree with only children
                this._buildTree(children);
            }
        });
    }

    _transformData(data) {
        return data.map((node) => this._transformNode(node));
    }

    _transformNode(node) {
        let nodeText;
        if (node.isTrashcan && node.isTrashcan()) {
            nodeText = `<span class="no-simple-browsing trashcan-node">Trash Can</span>`;
        } else {
            nodeText = `<span class="no-simple-browsing">`+
                            this.JFrogUIUtils.getSafeHtml(node.text)+
                       `</span>`;
        }

        let modifiedNode = {
            text: nodeText,
            data: node,
            type: node.iconType,
            children: node.hasChild && (node.isFolder() || node.isRepo()),
            li_attr: node.isTrashcan && node.isTrashcan() || (node.isInTrashcan && node.isInTrashcan() && node == this.selectedNode) ?
            {class: "-the-trashcan"} : node.isFavorite && node.isFavorite() ? {class: '-favorite'} : {},
            state: {
                selected: this.selectedNode === node
            }
        };

        let type = (typeof node.fileType != 'undefined' ? node.fileType : node.type);
        // replace the node icon type to the package type if necessary
        if(this._iconsShouldBeReplacedWithRepoPkgTypeIcon(type,node.repoPkgType,node.fullpath)){
            modifiedNode.type = node.iconType = node.repoPkgType.toLocaleLowerCase();
        }

        return modifiedNode;

    }

    _toggleCompactFolders() {
        this.treeBrowserDao.invalidateRoots();
        this.initJSTree();
    }

    _openNode(treeNode, loadNodeToViewAllowed) {
        this.artifactoryState.setState('tree_touched', true);

        if (loadNodeToViewAllowed) {
            this._loadNodeIntoView(treeNode);
        }
        else {
            // just select (no need to refresh current tree)
            this.selectedNode = treeNode;
            this._dispatchEvent();
        }
    }

    _registerTreeEvents() {
        // $(this.treeElement).on("search.jstree", (e, data) => this._onSearch(e, data));

        $(this.treeElement).on("mousedown", (e) => e.preventDefault());

        $(this.treeElement).on("ready.jstree", (e) => this._onReady(e));

        $(this.treeElement).on("close_node.jstree", (e, args) => {
            this.jstree().open_node(args.node);
        });

        // Selecting a node (by clicking its name)
        $(this.treeElement).on("select_node.jstree", (e, args) => {
            let treeNode = args.node.data;
            let loadNodeToViewAllowed = (!treeNode || // Going up to roots
            (this.currentParentNode &&
            this.currentParentNode.parent === treeNode));

            this._openNode(treeNode, loadNodeToViewAllowed);
        });

        // Click on the left triangle l>
        $(this.treeElement).on("load_node.jstree", (e, args)=> {
            let treeNode = args.node.data;
            let loadNodeToViewAllowed = (treeNode !== this.currentParentNode && // Not selecting the current parent
            (!treeNode || // Going up to roots
            treeNode.isFolder() || treeNode.isRepo())); // drilling down to folder or repo

            this._openNode(treeNode, loadNodeToViewAllowed);
        });

        $('#tree-element').on('keydown',(e) => {
            if (e.keyCode === 13) {
                let node = this.jstree().get_selected(true)[0];
                this.jstree().toggle_node(node);
            }
        });

        $(this.treeElement).scroll(()=>this._onScroll());
    }

    _dispatchEvent() {
        if (!this.selectedNode) {
            return;
        }
        // Make sure tree data is loaded
        this.selectedNode.load().then(() => {
            // Then dispatch TREE_NODE_SELECT event
            this.JFrogEventBus.dispatch(this.EVENTS.TREE_NODE_SELECT, {data: this.selectedNode});
        });
    }

    _patchJSTreeTriggerFunction() {
        let jstree = this.jstree();
        let origTrigger = jstree.trigger;
        let newTrigger = function() {
            if (this.element) {
                origTrigger.apply(this, arguments);
            }
            else {
                this.redraw_node = () => {};
                this._redraw = () => {};
                this.get_node = () => {return {removeClass: ()=>({attr: ()=>{}})}};
                this.get_container_ul = () => $('<ul></ul>')
            }
        }
        jstree.trigger = newTrigger.bind(jstree);
    }

    _onReady() {

        this.jstree().show_dots();
        this._focusOnTree();
        $('a.jstree-clicked').focus();
    }


    /****************************
     * Event registration
     ****************************/
    _registerEvents() {
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_EDIT, () => this._edit_search());
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CHANGE, text => this._searchTree(text)
        )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CANCEL, text => this._clear_search()
        )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_KEYDOWN,
                key => this._searchTreeKeyDown(key)
        )
        ;

        // Must destroy jstree on scope destroy to prevent memory leak:
        this.$scope.$on('$destroy', () => {
            if (this.jstree()) {
                this.jstree().destroy();
            }
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DEPLOY, (eventArgs) => {
            this.artifactoryState.setState('tree_touched', true); // Make sure we go inside the repo and not stay at the root level
            let repoKey = eventArgs[0],
                targetPath = eventArgs[1].targetPath;
            let fullpath = _.compact([repoKey, targetPath]).join('/');
            this.treeBrowserDao.invalidateRoots();
            this._loadNodeByPath(fullpath);
            // console.log(repoKey,targetPath,fullpath);

            // this.treeBrowserDao.findRepo(repoKey)
            // .then((repoNode) => {
            //     this._loadNodeByPath(repoNode.fullPath);
            // });
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_REFRESH, (node) => {
            this._refreshFolder(node);
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH_SORTING, ()=>this._refreshTreeSorting());

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH_FILTER, ()=>this._refreshTreeFiltering());

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH_FAVORITE_STATE, (node)=>this._refreshFavoriteState(node));

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DELETE, (node) => {
            this._refreshOnDelete(node);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DELETE_CONTENT, (node) => {
            this._refreshOnDelete(node);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_MOVE, (options) => {
            this._openTargetNode(options)
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_COPY, (options) => {
            this._openTargetNode(options)
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_COMPACT, () => this._toggleCompactFolders()
        );

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TABS_URL_CHANGED, (stateParams) => {
            if (stateParams.browser != 'simple') {
                return;
            }
            // URL changed (like back button / forward button / someone input a URL)
            let currentNodePath = this.selectedNode && this.selectedNode.fullpath || '';
            if (currentNodePath != stateParams.artifact || stateParams.forceLoad) {
                if (!this.dontOpenFirstNode) {
                    this._loadNodeByPath(stateParams.artifact);
                }
                else {
                    this._selectNodeByPath(stateParams.artifact);
                }
                this.dontOpenFirstNode = false;
            }
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.REFRESH_PAGE_CONTENT,()=>{
            if(this.currentParentNode == null){
                this._refreshTree();
            }else{
                this.artifactoryState.setState('refreshTreeWhenBackToRoot',true);
            }
        });

    }

    _openTargetNode(options) {
        this.$timeout(() => {
            let fullpath = _.compact(
                    [options.target.targetRepoKey, options.target.targetPath, options.node.data.text]).join('/');
            this.treeBrowserDao.invalidateRoots();
            this._loadNodeByPath(fullpath);
        }, 500);
    }


    /****************************
     * Build the JSTree from the nodes
     ****************************/
    _buildTree(data) {
        let hasArtifactsData = data.length > 0;
        this.artifactoryState.setState("hasArtifactsData", hasArtifactsData);
        this.JFrogEventBus.dispatch(this.EVENTS.TREE_DATA_IS_SET, hasArtifactsData);

        let filteredData = data.filter(n => {
            return this._shouldShowNode(this.searchText, n);
        });
        if (hasArtifactsData) {
            TreeConfig.core.data = data;

            TreeConfig.contextmenu.items = this._getContextMenuItems.bind(this);

            if (this.built) {
                this.jstree().destroy();
            }
            $(this.treeElement).jstree(TreeConfig);
            this._patchJSTreeTriggerFunction();
            if (this.selectedNode && (this.activeFilter || this.getPersistentFilter())) {
                this._filterTree();
                this._searchTree(this.searchText, false, false);
            }

            this.built = true;
            this._registerTreeEvents();
            if (!filteredData.length) {
                this._filterTree();
            }
        }
    }


    // setCurrentTab(tab) {
    //     this.currentTab = tab;
    // }

    // isCurrentTab(tab) {
    //     return this.currentTab === tab;
    // }
    /****************************
     * Refreshing the tree
     ****************************/

    /**
     * refresh children of viewed folder
     *
     * @param node
     * @private
     */

    _refreshTree() {
        this.treeBrowserDao.invalidateRoots();
        if (this.jstree() && this.jstree().refresh) {
            this.jstree().refresh();
        }
    }

    _refreshOnDelete(deletedNode) {
        this.$timeout(() => {
            if (deletedNode) {
                this.selectedNode = deletedNode.data;
            }

            // Simple tree is only 2 levels deep (makes delete easy - root changes every time a node is loaded)
            let root = TreeConfig.core.data[1];
            let deletedNodeIsLastChild = (root.children.length == 1);
            let deletedNodeIsRoot = (this._deletedNodeIsTheViewedNode(deletedNode,root));

            if(deletedNodeIsLastChild && !deletedNodeIsRoot){
                this.onLastChildDelete(deletedNode,root);
            }
            else if(deletedNodeIsRoot){
                deletedNode.data.invalidateParent();
                this._loadNodeIntoView(deletedNode.data.parent);
            }
            else{
                this._refreshParentFolder(deletedNode);
            }

            this._dispatchEvent()

        }, 500);
    }

    _deletedNodeIsTheViewedNode(deletedNode,root){
        return (root.data && deletedNode.data && root.data.fullpath === deletedNode.data.fullpath);
    }

    hasParentFolder(node) {
        return (node
        && node.parent
        && typeof this.currentParentNode.parent !== 'undefined'
        && node.data.parent.isFolder());
    }

    onLastChildDelete(deletedNode,treeRoot){
        // If parent exists and is folder - traverse up
        if (this.hasParentFolder(deletedNode)) {
            this.currentParentNode = deletedNode.data.parent;

            let rootParent = treeRoot.data.parent;
            rootParent.invalidateChildren();
            this._loadNodeIntoView(rootParent);
        }
        // Traverse to absolute tree root
        else {
            this.treeBrowserDao.invalidateRoots();
            this._loadNodeIntoView(this.currentParentNode.parent);
        }
    }

    _refreshOnDeploy(repoNode) {
        let repoWasEmptyBeforeDeployment = !repoNode.hasChild;
        let currentParentNode = this._getSelectedNodeTreeObject();

        this.$timeout(() => {
            // If the destination folder of deployed file is empty => refresh its parent two
            if (repoWasEmptyBeforeDeployment) {
                // If this is an empty repo at root level - refresh the tree
                if (this._viewedNodeIsRepo()) {
                    this._refreshTree();
                }
                // If this is sn empty folder not at root level - traverse to parent
                else {
                    this._refreshParentFolder(currentParentNode.parent);
                }
            }
            // Else => refresh the node itself
            else {
                let folderToRefresh = this._getViewedNodeTreeObject();
                this._refreshFolder(folderToRefresh);
            }
        }, 800);
    }

    _refreshFolder(node) {
        // console.log(node.data,this.currentParentNode);
        if (node.data) {
            node.data.invalidateChildren();
        } else {
            this.treeBrowserDao.invalidateRoots();
        }

        if (node.data != this.currentParentNode) {
            return;
        }
        this._loadNodeIntoView(node.data);
    }

    _refreshFavoriteState(node) {
        let isFavorite = node.isFavorite();
        node = this._getNodeByFullPath(node.fullpath);
        let elem = this.jstree().get_node(node, true);
        if (isFavorite) {
            elem.removeClass('jstree-li');
            elem.addClass('jstree-li-favorite');
        }
        else {
            elem.addClass('jstree-li');
            elem.removeClass('jstree-li-favorite');
        }
    }

    _refreshParentFolder(node) {
        if (node.data) {
            node.data.invalidateChildren();
            node.data.invalidateParent();
        }
        let parentNodeItem = this.jstree().get_node(node.parent);
        this.$timeout(() => {
            this._refreshFolder(parentNodeItem);
            this.jstree().select_node(parentNodeItem);
        }, 500);
    }

    _refreshTreeSorting() {
        if (this.jstree() && this.jstree().refresh) {
            this.jstree().refresh();
        }
    }

    _refreshTreeFiltering() {
        this._filterTree();
        this._autoSelectFirstIfSelectionNotVisible();
    }

    _autoSelectFirstIfSelectionNotVisible() {
        this.$timeout(() => {
            let selected = this.jstree().get_node(this.jstree().get_selected());
            if (!this._shouldShowNode(this.searchText, selected)) {
                let theTree = $('#tree-element');
                let firstNode = theTree.find('.jstree-node:not(.hidden)').first();
                firstNode = this.jstree().get_node(firstNode.attr('id'));
                this.$stateParams.tab = 'General';
                this.$stateParams.artifact = firstNode.type === 'go_up' ? '' : firstNode.data ? firstNode.data.fullpath || '' : '';
                this.whenTreeDataLoaded = this.treeBrowserDao.findNodeByFullPath(this.$stateParams.artifact);
                this.dontOpenFirstNode = true;
                this.$state.transitionTo('artifacts.browsers.path',this.$stateParams,{ location: 'replace', inherit: true, notify: false });
            }
        })
    }

    _viewedNodeIsRepo() {
        return this.currentParentNode === null || typeof this.currentParentNode.parent === 'undefined';
    }

    _getSelectedNodeTreeObject() {
        return this.jstree().get_node(this._getSelectedNode());
    }

    _getViewedNodeTreeObject() {
        let selected = this._getSelectedNodeTreeObject();
        return selected.data !== null && selected.data.hasChild ? selected : this.jstree().get_node(selected.parent);
    }
}

export function jfSimpleBrowser() {
    return {
        scope: {
            browserController: '='
        },
        restrict: 'E',
        controller: JfSimpleBrowserController,
        controllerAs: 'SimpleBrowser',
        bindToController: true,
        link: ($scope, attrs, $element, SimpleBrowser) => SimpleBrowser.initJSTree(),
        templateUrl: 'states/artifacts/jf_simple_browser/jf_simple_browser.html'
    }
}

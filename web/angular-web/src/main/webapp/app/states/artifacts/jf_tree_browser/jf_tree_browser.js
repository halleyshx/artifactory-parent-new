import TreeConfig from "./jf_tree_browser.config";
import JFCommonBrowser from "../jf_common_browser/jf_common_browser";
/**
 * wrapper around the jstree jquery component
 * @url http://www.jstree.com/
 *
 * @returns {{restrict: string, controller, controllerAs: string, bindToController: boolean}}
 */
export function jfTreeBrowser() {
    return {
        scope: {
            browserController: '='
        },
        restrict: 'E',
        controller: JFTreeBrowserController,
        controllerAs: 'jfTreeBrowser',
        templateUrl: 'states/artifacts/jf_tree_browser/jf_tree_browser.html',
        bindToController: true,
        link: function ($scope) {
            $scope.jfTreeBrowser.initJSTree();
        }
    }
}

const ARCHIVE_MARKER = '!';

class JFTreeBrowserController extends JFCommonBrowser {
    constructor($state,$timeout, $compile, JFrogEventBus, $element, $scope, TreeBrowserDao, $stateParams, $q,
                ArtifactoryState, ArtifactActions, JFrogNotifications, NativeBrowser, User, AdvancedStringMatch,
                JFrogUIUtils) {
        super(ArtifactActions, AdvancedStringMatch, ArtifactoryState);
        this.type="tree";
        this.$state = $state;
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.$compile = $compile;
        this.$stateParams = $stateParams;
        this.$q = $q;
        this.artifactoryNotifications = JFrogNotifications;
        this.JFrogEventBus = JFrogEventBus;
        this.JFrogUIUtils = JFrogUIUtils;
        this.EVENTS = JFrogEventBus.getEventsDefinition();
        this.treeBrowserDao = TreeBrowserDao;
        this.artifactoryState = ArtifactoryState;
        this.user = User.currentUser;
        this.nativeBrowser = NativeBrowser;

        if (this.browserController) this.browserController.currentBrowser = this;

        this.spinnerTimeout = this.$timeout(()=>{
            if ($state.current.name === 'artifacts.browsers.path') {
                this.JFrogEventBus.dispatch(this.EVENTS.SHOW_SPINNER);
            }
        },600);
        if (_.isUndefined($stateParams.artifact)) {
            // Important to know for switching to simple browser
            this.whenTreeDataLoaded = $q.when([]);

            // Handle a case of no artifact and tab properties in state params
            this.dataLoaded = () => {
                let theTree = $('#tree-element');
                let firstNode = theTree.find('.jstree-node:not(.hidden)').first();

                $stateParams.tab = 'General';
                $stateParams.artifact = firstNode.text();
                this.whenTreeDataLoaded = TreeBrowserDao.findNodeByFullPath($stateParams.artifact);

                // Change the state URL (without redirecting)
                this.$state.transitionTo('artifacts.browsers.path',this.$stateParams,{ location: 'replace', inherit: true, notify: false });

                // Unbind
                this.dataLoaded = ()=>{};
            }
        }
        else {
            // Preload data for the current selected artifact
            this.whenTreeDataLoaded = TreeBrowserDao.findNodeByFullPath($stateParams.artifact)
                    .then(node => {
                        if (node !== TreeBrowserDao) {
                            node = this._transformData([node])[0];
                            if (!this._shouldShowNode(this.searchText, node)) {
                                this.browserController.resetAllFilters();
                            }
                        }
                    })
        }
        this.$element = $element;

        let doRefresh = this.artifactoryState.getState('refreshTreeNextTime');
        if (doRefresh) {
            $timeout(()=>this._refreshTree());
            this.artifactoryState.setState('refreshTreeNextTime',false);
        }

    }

    dataLoaded(){}

    /****************************
     * Init code
     ****************************/

    // This is called from link function
    initJSTree() {
        // preload artifact
        this.whenTreeDataLoaded.then(() => {
            this.treeElement = $(this.$element).find('#tree-element');
            this._registerEvents();
            this._buildTree();
            this._registerTreeEvents();
        });
    }

    /**
     * When JStree is ready load the current browsing path from URL
     * and restore the nodes open and selected state.
     * @param e
     * @private
     */
    _openTreeNode(artifact) {
        let deferred = this.$q.defer();
        let jstree = this.jstree();
        let root = jstree.get_node('#');
        let path = _.trim(artifact?artifact.replace('//', '/'):'', '/').split('/');

        let leaf = _.find(root.children, child => {
            return this._isVisible(jstree.get_node(child));
        })

        this._openNodePath(root, path, leaf, (selectedNode) => {
            jstree.deselect_all();
            // Select the node
            jstree.select_node(selectedNode);

            // scroll the node into view
            let domElement = this._getDomElement(selectedNode);
            this._ensureVisibility(domElement);
            this._scrollIntoView(domElement);
            this._focusOnTree();
            deferred.resolve();
        });
        return deferred.promise
    }

    selectRepo(artifact){
        let jstree = this.jstree();
        let pathStopIndex = 1;
        let path = _.trim(artifact?artifact.replace('//', '/'):'', '/').split('/');
        let root = jstree.get_node('#');

        let childNode;
        while(pathStopIndex <= path.length) {
            let testPath = path.slice(0, pathStopIndex);
            childNode = this._getChildByPath(root, testPath);
            if (childNode) break;
            pathStopIndex++;
        }
        jstree.deselect_all();
        jstree.select_node(childNode);
        let domElement = this._getDomElement(childNode);
        this._ensureVisibility(domElement);

        this._scrollIntoView(domElement);
        this._focusOnTree();
    }

    _onReady() {
        this.$timeout(()=>{
            this._initTrashPin();
            this.$timeout.cancel(this.spinnerTimeout);

            this.JFrogEventBus.dispatch(this.EVENTS.HIDE_SPINNER);
        },100);

        /**   Restore prev tree state  **/
        let artifact = this.$stateParams.artifact;
        // No repo was selected  before returning to artifacts page
        if(!artifact || artifact === ""){
            this._openTreeNode('');
        }
        // Selected repo was close before returning to artifacts page
        else if((artifact.indexOf('/') === -1)){
            this.selectRepo(artifact);
        }
        // Repo was open before returning to artifacts page
        else {
            this._openTreeNode(artifact);
        }

        this.jstree().show_dots();
        $('a.jstree-clicked').focus();
    }

    /****************************
     * Event registration
     ****************************/
    _registerEvents() {
        // Must destroy jstree on scope destroy to prevent memory leak:
        this.$scope.$on('$destroy', () => {
            if (this.jstree()) {
                this.jstree().destroy();
            }
            $(window).off('resize');
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_IGNORE_ALERT, (node)=>{
            let options ={
                target:{
                    targetRepoKey:node.data.repoKey,
                    targetPath: node.data.path
                },
                node:node,
            };
            this._refreshFolderPath(options); // Refresh target folder where node was copied
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_UNIGNORE_ALERT, (node)=>{
            let options ={
                target:{
                    targetRepoKey:node.data.repoKey,
                    targetPath: node.data.path
                },
                node: node,
            };
            this._refreshFolderPath(options); // Refresh target folder where node was copied
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CHANGE, text => this._searchTree(text)
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CANCEL, text => this._clear_search());
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_EDIT, () => this._edit_search());
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_KEYDOWN, key => this._searchTreeKeyDown(key)
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DEPLOY, (eventArgs) => {
            let repoKey = eventArgs[0];
            let repoJsNode = this._getNodeFromRepoKey(repoKey);
            repoJsNode.data.invalidateParent();
            this._refreshRepo(repoKey);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_REFRESH, node => this._refreshFolder(node)
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH, (node) => node ? this._refreshFolder(node) : this._refreshTree());

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH_SORTING, ()=>this._refreshTreeSorting());
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_REFRESH_FILTER, ()=>this._refreshTreeSorting());

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DELETE, (node) => {
            if (node.data.parent) node.data.parent.invalidateParent();
            this._refreshParentFolder(node); // Refresh folder of node's parent
            this.refreshTrashCan();
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DELETE_CONTENT, (node) => {
            node.data.invalidateParent();
            this._refreshFolder(node); // Refresh folder of node's parent
            this.refreshTrashCan();
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_MOVE, (options) => {
            this._refreshParentFolder(options.node); // Refresh folder of node's parent
            this._refreshFolderPath(options); // Refresh target folder where node was copied
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_COPY, (options) => {
            this._refreshFolderPath(options);
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_NODE_OPEN, path => {
            this._openTreeNode(path)
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_COLLAPSE_ALL, () => {
            this._collapseAll();
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_COMPACT, () => this._toggleCompactFolders()
    )
        ;

        // URL changed (like back button / forward button / someone input a URL)
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TABS_URL_CHANGED, (stateParams) => {
            if (stateParams.browser != 'tree') return;
            // Check if it's already the current selected node (to prevent opening the same tree node twice)
            let selectedNode = this._getSelectedTreeNode();
            if (selectedNode && selectedNode.fullpath === stateParams.artifact) return;
            this.treeBrowserDao.findNodeByFullPath(stateParams.artifact)
                .then(() => {
                    if (!this.dontOpenFirstNode) {
                        this._openTreeNode(stateParams.artifact)
                    }
                    else {
                        this.selectRepo(stateParams.artifact)
                    }
                    this.dontOpenFirstNode = false;
                });
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.REFRESH_PAGE_CONTENT,()=>{
            this._refreshTree();
        });

        $(window).on('resize', () => this._resizePinnedTrash());
        this.$scope.$on('ui-layout.resize', () => this._resizePinnedTrash());


    }

    /**
     * register a listener on the tree and delegate to
     * relevant methods
     *
     * @param element
     * @private
     */
    _registerTreeEvents() {
//        $(this.treeElement).on("search.jstree", (e, data) => this._onSearch(e, data));
        $(this.treeElement).on("ready.jstree", (e) => this._onReady(e));
        $(this.treeElement).on("mousedown",(e) => e.preventDefault());

        $(this.treeElement).on("select_node.jstree", (e, args) => {
            if (args.event) { // User clicked / pressed enter
                this.artifactoryState.setState('tree_touched', true);
            }
            // Allow scroll only after an artifact is selected (important for after tree is loaded)
            this.allowAutoScroll = true;
            this._loadNode(args.node);
        });

        $(this.treeElement).on("activate_node.jstree", (e, args) => {
            if (args.event) { // User clicked / pressed enter
                this.artifactoryState.setState('tree_touched', true);
            }
        });

        $(this.treeElement).on("after_open.jstree",(e, args)=>{
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash) this._initTrashPin();

            if (this.activeFilter || this.getPersistentFilter()) {
                this._filterTree();
                this._searchTree(this.searchText,false,false);
            }

            this._setScrolledTreeRegion(args.node);

            let elemToFocus = args.node.id;
            if(this.$autoScrollInTrash || this.$autoScroll){
                this._focusOnTree(elemToFocus);
                if(this.allowAutoScroll){
                    this._autoScroll(args.node,this.$autoScrollInTrash);
                }
            }
            else {
                this._focusOnTree();
            }
        });

        $(this.treeElement).on("after_close.jstree",(e, args)=> {
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash) this._initTrashPin();
        });
        $(this.treeElement).on("load_node.jstree",(e, args)=> {
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash || args.node.id === '#') this._initTrashPin();
            if (this.activeFilter || this.getPersistentFilter()) {
                this._filterTree();
                this._searchTree(this.searchText,false,false);
            }
        });


        $('#tree-element').on('keydown',(e) => {
            if (e.keyCode === 37 && e.ctrlKey) { //CTRL + LEFT ARROW --> Collapse All !
                this._collapseAll();
            }
            else if (e.keyCode === 13) {
                let node = this.jstree().get_selected(true)[0];
                this.jstree().toggle_node(node);
            }
        });

        $(this.treeElement).scroll(()=>this._onScroll());


    }

    /**
     * Determine for the current node whether the scroll should be from
     * the tree root or from the trashcan element
     *
     * @param node
     * @private
     * */
    _setScrolledTreeRegion(node){
        let nodeIsTrash = node.data && node.data.isTrashcan && node.data.isTrashcan();
        let nodeIsInTrash = node.data && node.data.isInTrashcan && node.data.isInTrashcan();

        this.$autoScroll = true;
        this.$autoScrollInTrash = false;

        if ((nodeIsTrash || nodeIsInTrash) && this.isTrashPinned()) {
            this.$autoScroll = false;
            this.$autoScrollInTrash = true;
        }
    }

    _autoScroll(node,scrollInTrash) {
        let treeItem = $('#' + node.id).find('.jstree-children')[0];
        if (!treeItem) return;

        let theTree = $('#tree-element');
        theTree = scrollInTrash ? theTree.find('.jstree-li-the-trashcan') : theTree;

        let numOfChildrens = $(treeItem).find('> div').length,
                heightOfElement = theTree.find('.jstree-anchor').first().height(),
                treeWrapperHeight = theTree.offset().top + theTree.height();

        if ($(treeItem).offset().top + (heightOfElement *2) > treeWrapperHeight) {
            let currentScroll = theTree.scrollTop();
            let sctollTo,addedScroll;

            if (numOfChildrens > 3) {
                addedScroll = ((heightOfElement * 3) + (heightOfElement/2) + 5);
            }
            if (numOfChildrens <= 3) {
                addedScroll = (heightOfElement * numOfChildrens);
            }
            // Scroll when trash is pinned
            if(scrollInTrash && this.isTrashPinned()){
                addedScroll = $(treeItem).position().top - heightOfElement;
            }

            sctollTo = currentScroll + addedScroll;

            this.$timeout(()=> {
                theTree.animate({scrollTop: sctollTo});
            });
        }
    }

    _resizePinnedTrash() {
        let e = $('.jstree-li-the-trashcan');
        if (e.hasClass('pinned')) {
            let p = e.parent().parent();
            e.css('width',p.outerWidth()+'px');

            this.treeElement.css('height','auto');
            var h = parseInt(this.treeElement.css('height'));
            this.treeElement.css('height',h-e.height() + 'px');

        }
        else {
            e.css('width','auto');
            this.treeElement.css('height','auto');
        }

        let trashPin = $('.trash-pin');
        trashPin.css('left', e.parent().parent().width() - 50 + 'px')


    }

    _collapseAll() {
        let node = this.jstree().get_selected(true)[0];

        let parentRepoNode = this.jstree().get_node(node.parent);
        if (parentRepoNode.id === '#') parentRepoNode = node;
        else {
            while (parentRepoNode.parent !== '#') {
                parentRepoNode = this.jstree().get_node(parentRepoNode.parent);
            }
        }

        $('#tree-element').jstree('close_all');
        this.jstree().select_node(parentRepoNode);
        this.$timeout(()=>{
            this.jstree().close_node(parentRepoNode);
        });
    }

    _loadNode(item) {
        item.data.load().then(() => {
            this.JFrogEventBus.dispatch(this.EVENTS.TREE_NODE_SELECT, item)
        });
    }

    /****************************
     * Compact folders
     ****************************/
    _toggleCompactFolders() {
        this._refreshTree();
    }

    /****************************
     * Building the tree
     ****************************/
    _buildTree() {
        let asyncStateLoad = (obj, cb) => {
            let promise;
            if (obj.id === '#') {
                promise = this.treeBrowserDao.getRoots();
            }
            else {
                promise = obj.data.getChildren();
            }
            promise
            .then((data) => {
                let hasArtifactsData = data.length > 0 || obj.id !== '#';
                this.artifactoryState.setState("hasArtifactsData",hasArtifactsData);
                this.JFrogEventBus.dispatch(this.EVENTS.TREE_DATA_IS_SET,hasArtifactsData);
                cb(this._transformData(data));
            })
            .then(this.dataLoaded);
        };

        TreeConfig.core.data = asyncStateLoad;
        TreeConfig.contextmenu.items = this._getContextMenuItems.bind(this);

        $(this.treeElement).jstree(TreeConfig);
    }

    _transformData(data) {
        data = data || [];
        return data.map((node) => {
            let item = {};
            item.children = node.hasChild;
            item.text = node.isTrashcan() ? '<span class="trashcan-node">Trash Can<i ng-show="!jfTreeBrowser.isTrashPinned()" ng-click="jfTreeBrowser.toggleTrashPin($event)" class="icon icon-pin trash-pin" jf-tooltip="Pin Trash Can"></i><i ng-show="jfTreeBrowser.isTrashPinned()" ng-click="jfTreeBrowser.toggleTrashPin($event)" class="icon icon-unpin trash-pin" jf-tooltip="Unpin Trash Can"></i></span>'
                    : this.JFrogUIUtils.getSafeHtml(node.text);
            item.data = node;
            item.type = node.iconType;
            if (node.isTrashcan()) {
                item.li_attr={class:"-the-trashcan"};
            }

            if (node.isFavorite()) {
                item.li_attr={class:"-favorite"};
            }

            let type = (typeof node.fileType != 'undefined' ? node.fileType : node.type);
            // replace the node icon type to the package type if necessary
            if(this._iconsShouldBeReplacedWithRepoPkgTypeIcon(type,node.repoPkgType,node.fullpath)){
                item.type = node.iconType = node.repoPkgType.toLocaleLowerCase();
            }

            return item;
        });
    }


    /****************************
     * Refreshing the tree
     ****************************/

    /**
     * refresh children of folder
     *
     * @param node
     * @private
     */
    _refreshRepo(repoKey) {

        let repoJsNode = this._getNodeFromRepoKey(repoKey);
        if (repoJsNode) {
            if (!this._shouldShowNode(this.searchText, repoJsNode)) {
                this.browserController.resetAllFilters();
            }
            repoJsNode.data.invalidateChildren();
            this.jstree().load_node(repoJsNode, () => {
                this.jstree().select_node(repoJsNode);
            });
        }
    }

    _getNodeFromRepoKey(repoKey) {
        let jstree = this.jstree();
        let root = jstree.get_node('#');
        let repoJsNode;
        _.each(root.children, (child) => {
            repoJsNode = jstree.get_node(child);
            if (repoJsNode && repoJsNode.data && repoJsNode.data.repoKey === repoKey) return false;
        });
        return repoJsNode;
    }

    _refreshFolder(node) {
        if (!this._shouldShowNode(this.searchText, node)) {
            this.browserController.resetAllFilters();
        }
        let defer = this.$q.defer();
        if (node.data) node.data.invalidateChildren();
        else this.treeBrowserDao.invalidateRoots();
        this.jstree().load_node(node,()=>defer.resolve());
        return defer.promise;
    }

    _refreshParentFolder(node) {
        node.data.invalidateParent();
        let parentNodeItem = this.jstree().get_node(node.parent);
        let openedChildrenPaths = this._getOpenedChildrenPaths(parentNodeItem);
        this.$timeout(() => {
            this._refreshFolder(parentNodeItem).then(()=>{
                this._openChildrenByPaths(parentNodeItem, openedChildrenPaths)
                this.$timeout(() => {
                    this.jstree().hover_node(parentNodeItem);
                    this.jstree().select_node(parentNodeItem);
                });
            })
        });
    }

    _getOpenedChildrenPaths(parent) {
        let children = parent.children;
        let jst = this.jstree();
        let opened = [];
        children.forEach(child=>{
            let n = jst.get_node(child);
            if (n.state.opened && n.data && (n.data.path || n.data.type === 'repository')) {
                opened.push(n.data.path || n.data.repoKey);
                opened = opened.concat(this._getOpenedChildrenPaths(n));
            }
        });
        return opened;
    }

    _openChildrenByPaths(parent, pathsToOpen) {
        let children = parent.children;
        let jst = this.jstree();
        children.forEach(child=>{
            let n = jst.get_node(child);
            if (n.data && _.includes(pathsToOpen,(n.data.path  || n.data.repoKey))) {
                jst.open_node(n,()=>{
                    this._openChildrenByPaths(n, pathsToOpen);
                });
            }
        });
    }

    _refreshFolderPath(option) {
        let targetPath = _.compact(option.target.targetPath.split('/'));
        let path = [option.target.targetRepoKey].concat(targetPath);

        let curNode = this.jstree().get_node('#');

        let childNode = this._getChildByPath(curNode, path);
        if (childNode && _.isArray(childNode.children)) {
            curNode = childNode;
        }

        // Data is still not refreshed on server
        this.$timeout(()=> {
            if (curNode && curNode.data) {
                this._refreshFolder(curNode);
                curNode.data.getChildren().then(()=> {
                    this._openTreeNode(option.target.targetRepoKey + '/' + option.target.targetPath + '/' + option.node.data.text)
                });
            }
            else {
                this._openTreeNode(option.target.targetRepoKey + '/' + option.target.targetPath + '/' + option.node.data.text);
            }
        }, 500);
    }

    _refreshTree() {
        this.treeBrowserDao.invalidateRoots();
        if (this.jstree() && this.jstree().refresh) this.jstree().refresh();
    }

    _refreshTreeSorting() {
        this.browserController.filterHasNoMatches = false;
        if (this.jstree() && this.jstree().refresh) this.jstree().refresh();
        this.$timeout(() => {
            let selectedNode = this.jstree().get_node(this._getSelectedNode())
            if (!this._shouldShowNode(this.searchText, selectedNode)) {
                let theTree = $('#tree-element');
                let firstNode = theTree.find('.jstree-node:not(.hidden)').first();
                this.$stateParams.tab = 'General';
                this.$stateParams.artifact = firstNode.text();
                this.whenTreeDataLoaded = this.treeBrowserDao.findNodeByFullPath(this.$stateParams.artifact);
                this.dontOpenFirstNode = true;
                this.$state.transitionTo('artifacts.browsers.path',this.$stateParams,{ location: 'replace', inherit: true, notify: false });
            }
        })
    }

    /****************************
     * Traversing the tree
     ****************************/

     /**
     * Find the next child by path. Take into account the node's text by consist of some of the path elements (in compact mode)
     * @param parentNode:Object node object from where to start
     * @param path:Array array of path elements
     * @returns childNode or undefined
     * @private
     */
    _getChildByPath(parentNode, path) {
        let jstree = this.jstree();
        let children = this._getChildrenOf(parentNode);
        // Find the node that conforms to the largest subpath of path
        for(let i = path.length; i > 0; i--) {
            let subpath = path.slice(0, i);
            let testPathStr = _.trimRight(subpath.join('/'), ARCHIVE_MARKER);
            let result = _.find(children, (childNode) => {
                // Sometimes the node's text is not the full text (like for docker images)
                let childPath = childNode.data.fullpath;

                if (childPath === testPathStr || childPath === testPathStr + '/') {
                    return childNode;
                }
            });
            if (result) return result;
        }
    }

    _getChildrenOf(parentNode) {
        let jstree = this.jstree();
        return _.compact(parentNode.children.map((jsTreeNodeId) => jstree.get_node(jsTreeNodeId)));
    }

    /**
     * Open the path starting from the root node, and call the callback with the leaf node
     * and restore the nodes open and selected state.
     * @param node:Object node object from where to start
     * @param path:Array array of path elements
     * @param selectedNode:Object default node to return if not found
     * @param callback:Function callback to call with leaf node once the traversing is complete
     * @private
     */
    _openNodePath(node, path, leafNode, callback, pathStopIndex = 1) {
        let jstree = this.jstree();
        let childNode;
        while(pathStopIndex <= path.length) {
            let testPath = path.slice(0, pathStopIndex);
            childNode = this._getChildByPath(node, testPath);
            if (childNode) break;
            pathStopIndex++;
        }

        if (childNode) {
            leafNode = childNode;
            if (path.length === 0) {
                callback(leafNode);
            }
            else {
                if (this._isNodeShouldOpenOnClick(leafNode.data)) {
                    if (!this._shouldShowNode(this.searchText, leafNode)) {
                        this.browserController.resetAllFilters();
                    }
                    this.$timeout(() => {
                        jstree.open_node(leafNode, (node) => {
                            this._openNodePath(leafNode, path, leafNode, callback, pathStopIndex + 1);
                        }, false);
                    })
                }
                else {
                    callback(leafNode);
                }
            }
        }
        else {
            callback(leafNode);
        }
    }

    _isNodeShouldOpenOnClick(nodeData) {
        return !nodeData.isArchive() && nodeData.icon !== 'docker'&& nodeData.icon !== 'conan'
    }

    refreshTrashCan() {
        let trashID = $('.trashcan-node').parent().parent().prop('id');
        let trashNode = this.jstree().get_node(trashID);
        this.JFrogEventBus.dispatch(this.EVENTS.TREE_REFRESH, trashNode);
    }

    pinTrash() {
        let trashElem = $('.jstree-li-the-trashcan');
        if (!trashElem.length) return;

        let trashPin = $('.trash-pin');
        this.tempScrollTop = trashElem.scrollParent().scrollTop();
        let wasPinned = trashElem.hasClass('pinned');
        trashElem.addClass('pinned');
        localStorage.pinnedTrash = true;

        this.$scope.$broadcast('ui-layout.resize');
        trashElem.on('scroll', () => {
            trashPin.css('left', trashElem.scrollLeft() + trashElem.parent().parent().width() - 50 + 'px')
        });
        if (!wasPinned) trashPin.scrollParent().scrollTop(0);

    }

    unpinTrash() {
        let trashElem = $('.jstree-li-the-trashcan');
        if (!trashElem.length) return;

        trashElem.removeClass('pinned');
        localStorage.pinnedTrash = false;
        trashElem.scrollParent().scrollTop(this.tempScrollTop);

        if (!this._isScrolledIntoView(trashElem.find('.jstree-anchor')[0],0)) {
            this._scrollIntoView($(trashElem.find('.jstree-anchor')[0]));
        }

        this.$scope.$broadcast('ui-layout.resize');
    }

    toggleTrashPin(e) {
        e.stopImmediatePropagation()
        e.preventDefault();

        let trashElem = $('.jstree-li-the-trashcan');
        if (trashElem.hasClass('pinned')) {
            this.unpinTrash();
        }
        else {
            this.pinTrash();
       }
    }

    _initTrashPin() {
        let e = $('.trash-pin');
        if (!e.prop('compiled')) {
            this.$compile(e)(this.$scope);
            e.prop('compiled',true);
        }

        if (this.isTrashPinned()) {
            this.pinTrash();
        }
        this.$scope.$broadcast('ui-layout.resize');
    }

    isTrashPinned() {
        return localStorage.pinnedTrash !== undefined ? localStorage.pinnedTrash === 'true' : true;
    }

}
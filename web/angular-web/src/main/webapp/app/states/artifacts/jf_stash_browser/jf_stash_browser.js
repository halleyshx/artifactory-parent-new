import TreeConfig from "./jf_stash_browser.config";
import JFCommonBrowser from "../jf_common_browser/jf_common_browser";
/**
 * wrapper around the jstree jquery component
 * @url http://www.jstree.com/
 *
 * @returns {{restrict: string, controller, controllerAs: string, bindToController: boolean}}
 */
export function jfStashBrowser() {
    return {
        scope: {
            startCompact: '='
        },
        restrict: 'E',
        controller: JFStashBrowserController,
        controllerAs: 'jfStashBrowser',
        templateUrl: 'states/artifacts/jf_stash_browser/jf_stash_browser.html',
        bindToController: true,
        link: function ($scope) {
            $scope.jfStashBrowser.initJSTree();
        }
    }
}

class JFStashBrowserController extends JFCommonBrowser {
    constructor($timeout, $injector, JFrogEventBus, $element, $scope, $state, $stateParams, $location, $q, ArtifactoryState, ArtifactActions, StashResultsDao, User, AdvancedStringMatch, JFrogUIUtils) {
        super(ArtifactActions, AdvancedStringMatch, ArtifactoryState);

        this.rootID = '____root_node____';

        this.type="stash";
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.$location = $location;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.$q = $q;
        this.user = User;
        this.TreeNode = $injector.get('TreeNode');
        this.JFrogEventBus = JFrogEventBus;
        this.EVENTS = JFrogEventBus.getEventsDefinition();
        this.JFrogUIUtils  = JFrogUIUtils;
        this.stashResultsDao = StashResultsDao;

        this.whenTreeDataLoaded = $q.when([]);

        this.$element = $element;

        this.compactMode = this.startCompact || false;

        this.filteredActions = ['Copy', 'Move', 'Watch', 'Unwatch', 'UploadToBintray', 'Distribute', 'Refresh', 'DeleteVersions', 'DownloadFolder', 'Zap', 'ZapCaches', 'IgnoreAlert', 'UnignoreAlert'];

        this.discardedCount = 0;
    }


    /****************************
     * Init code
     ****************************/

    // This is called from link function
    initJSTree() {
        this.whenTreeDataLoaded.then(() => {
            this.treeElement = $(this.$element).find('#tree-element');
            this._registerEvents();
            this._buildStashedTree();
        });
    }

    _onReady() {
        let currentPath = this.$stateParams.artifact ? this.$stateParams.artifact.substring(this.$stateParams.artifact.indexOf('/')+1).split(' ').join('') : null;

        if (currentPath) {
            this.$timeout(()=>{
                this.jstree().deselect_all();
                this.jstree().select_node(currentPath);
                this.jstree().open_node(currentPath);

                let domElement = this._getDomElement(currentPath);
                this._scrollIntoView(domElement);
                this._focusOnTree();
            })
        }
        else {
//            this.JFrogEventBus.dispatch(EVENTS.TREE_NODE_SELECT,this._getSelectedTreeNode());
            this.jstree().select_node(this.rootID);
            this.jstree().open_node(this.rootID);
        }
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
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CHANGE, text => this._searchTree(text)
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_CANCEL, text => this._clear_search()
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_SEARCH_KEYDOWN, key => this._searchTreeKeyDown(key)
    )
        ;
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DELETE, (node) => {
            node.alreadyDeleted = true;
            this.artifactActions.perform({name: 'DiscardFromStash'},node);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_MOVE_STASH, (options) => {
            this.exitStashState(options);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_COPY_STASH, (options) => {
            this.exitStashState(options);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DISCARD_STASH, () => {
            this._buildStashedTree();
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_DISCARD_FROM_STASH, (node) => {
            this._discardFromStash(node);
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_REFRESH_STASH, () => {
            this._buildStashedTree();
        });
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.ACTION_EXIT_STASH, (node) => {
            if (node) this.jstree().select_node(node.id);
            this.$timeout(()=>this.exitStashState());
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_NODE_OPEN, path => {
            this._openTreeNode(path)
        });

        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TREE_COMPACT, (compact) => this._toggleCompactFolders(compact)
    )
        ;

        // URL changed (like back button / forward button / someone input a URL)
        this.JFrogEventBus.registerOnScope(this.$scope, this.EVENTS.TABS_URL_CHANGED, (stateParams) => {

            if (stateParams.browser !== 'stash' || !this.jstree()) return;

            let path;
            path = stateParams.artifact ? stateParams.artifact.substring(stateParams.artifact.indexOf('/')+1) : this.rootID;

            let selectedNode = this._getSelectedTreeNode();
            if (selectedNode && selectedNode.fullpath === stateParams.artifact) return;

            this.jstree().deselect_all();
            this.jstree().select_node(path);
            let domElement = this._getDomElement(path);
            this._scrollIntoView(domElement);
            this._focusOnTree();
        });
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
        $(this.treeElement).on("mousedown",(e) => e.preventDefault());
        $(this.treeElement).on("ready.jstree", (e) => this._onReady(e));
        $(this.treeElement).on("select_node.jstree", (e, args) => {
            if (this.$stateParams.tab === 'StashInfo' && args.node.id !== this.rootID) {
                this.$stateParams.tab = 'General';
            }
            this._loadNode(args.node);
        });
        $(this.treeElement).on("activate_node.jstree", (e, args) => {
            this.jstree().open_node(args.node);
        });

        $('#tree-element').on('keydown',(e) => {
            if (e.keyCode === 37 && e.ctrlKey) { //CTRL + LEFT ARROW --> Collapse All !
                let node = this.jstree().get_selected(true)[0];

                let parentRepoNode = this.jstree().get_node(node.parent);
                if (parentRepoNode.id === this.rootID) parentRepoNode = node;
                else {
                    while (parentRepoNode.parent !== this.rootID) {
                        parentRepoNode = this.jstree().get_node(parentRepoNode.parent);
                    }
                }

                $('#tree-element').jstree('close_all');
                this.jstree().select_node(parentRepoNode);
                this.$timeout(()=>{
                    this.jstree().close_node(parentRepoNode);
                });
            }
        })

        $(this.treeElement).scroll(()=>this._onScroll());

    }

    _loadNode(item) {
        if (item.data.load) item.data.load().then(() => {
            this.JFrogEventBus.dispatch(this.EVENTS.TREE_NODE_SELECT, item);
        });
    }


    /****************************
     * Compact folders
     ****************************/
    _toggleCompactFolders(compact) {
        this.compactMode = compact;
        this._buildStashedTree();
    }

    _buildStashedTree() {
        //TODO: stashResultsDao should also return data of archive for each resource and repoPkgType
        // and then the use of this. _iconsShouldBeRplacedWithRepoPkgType() could be made
        this.stashResultsDao.get({name: 'stash'}).$promise.then((data)=>{
            TreeConfig.core.data = this._transformStashDataToTree(data);
            TreeConfig.contextmenu.items = this._getContextMenuItems.bind(this);
            if (this.built) this.jstree().destroy();
//            TreeConfig.search.search_callback = this._searchCallback.bind(this);
            $(this.treeElement).jstree(TreeConfig);
            this.built = true;
            if (this.compactMode) {
                this._compactTree();
            }
            this._registerTreeEvents();
        });

    }

    _createRootNode(stashData) {
        let THIS = this;
        let node;
        node = {
            id: this.rootID,
            parent: '#',
            text: 'Stashed Search Results',
            type: 'stash',
            data: {
                text: 'Stashed Search Results',
                iconType: 'stash',
                load: function() {

                    THIS.$stateParams.artifact = '';
                    THIS.$stateParams.tab = 'StashInfo';


                    this.tabs = [{name: "StashInfo"}];
                    this.actions = stashData.length ? [
                        {title: "Copy Stash to Repository", name: "CopyStash", icon: "icon-copy"},
                        {title: "Move Stash to Repository", name: "MoveStash", icon: "icon-move"},
                        {title: "Discard Search Results", name: "DiscardStash", icon: "icon-delete-content"}
                    ] : [];
                    if (!THIS.user.currentUser.getCanDeploy()) {
                        this.actions.shift();
                        this.actions.shift();
                    }
                    this.info = {
                        artifactCount: stashData.length - THIS.discardedCount
                    };
                    return THIS.$q.when(this);
                },
                getDownloadPath: () => {return this.$q.when(this);},
                refreshWatchActions: () => {return this.$q.when(this);},
                isRepo: () => {return false;}
            }

        }
        return node;
    }

    _transformStashDataToTree(stashData) {

        let treeData = [];

        let pushToTree = (treeItemData) => {
            if (!_.findWhere(treeData,{id:treeItemData.id})) {
                treeData.push(treeItemData);
            }
        };

        pushToTree(this._createRootNode(stashData));

        stashData.forEach((result,index)=>{
            result.path = result.relativePath;
            result.text = result.name;
            result.type = 'file';

            let dirArray = (result.relativePath).split('/');
            dirArray.pop();

            let resultNode = {
                id: result.relativePath.split(' ').join(''),
                text: this.JFrogUIUtils.getSafeHtml(result.name),
                parent: dirArray.join('/').split(' ').join('') || this.rootID,
                type: result.mimeType,
                data: this._filterActions(new this.TreeNode(result))
            };

            // replace the node icon type to the package type if necessary
            let type = (typeof result.fileType != 'undefined' ? result.fileType : result.type);
            if(this._iconsShouldBeReplacedWithRepoPkgTypeIcon(type,result.repoPkgType,result.fullpath)){
                resultNode.type = resultNode.data.iconType = result.repoPkgType.toLocaleLowerCase();
            }

            pushToTree(resultNode);

            for (let i = dirArray.length-1; i>=0; i--) {
                let up = _.clone(dirArray);
                up.pop();

                let folderNode = {
                    id: dirArray.join('/').split(' ').join(''),
                    text: dirArray[i],
                    parent: up.join('/').split(' ').join('') || this.rootID,
                    type: 'folder',
                    data: this._filterActions(new this.TreeNode({
                        repoKey: result.repoKey,
                        path: dirArray.join('/'),
                        text: dirArray[i],
                        type: 'folder'
                    }))
                };

                pushToTree(folderNode);
                dirArray.pop();
            }

        });

        return treeData;
    }

    _compactTree() {

        let recursiveCompact;
        recursiveCompact = (node) => {
            if (node.type !== 'folder') {
                node.data = this.jstree().get_node(node.id).data;
                return;
            }

            if (node.children.length === 1 && node.children[0].type === 'folder') {
                node.text += '/' + node.children[0].text;

                if (this.$stateParams.artifact === node.data.repoKey+'/'+node.data.path) {
                    this.$stateParams.artifact = this.jstree().get_node(node.children[0].id).data.repoKey+'/'+this.jstree().get_node(node.children[0].id).data.path;
                }

                node.data = this.jstree().get_node(node.children[0].id).data;
                node.id = node.children[0].id;
                node.children = node.children[0].children;
                recursiveCompact(node);
            }
            else if (node.children.length > 0) {
                node.data = this.jstree().get_node(node.children[0].id).data;
                node.children.forEach((child) => {
                    child.data = this.jstree().get_node(child.id).data;
                    recursiveCompact(child)
                });
            }
        };

        let json = this.jstree().get_json();
        json[0].children.forEach((node) => recursiveCompact(node));

        TreeConfig.core.data = json;
        if (this.built) this.jstree().destroy();
        $(this.treeElement).jstree(TreeConfig);
    }

    _filterActions(treeNode) {
        let origLoad = treeNode.load.bind(treeNode);
        treeNode.load = () => {
            return origLoad().then(()=> {

                treeNode.actions = _.filter(treeNode.actions, (action)=> {
                    return this.filteredActions.indexOf(action.name) === -1;
                });

                let deleteAction = _.findWhere(treeNode.actions, {name: "Delete"});
                if (deleteAction) treeNode.actions.splice(treeNode.actions.indexOf(deleteAction), 1);


                if (!_.findWhere(treeNode.actions, {name: "ShowInTree"})) {
                    treeNode.actions.push({
                        title: "Show In Tree",
                        name: "ShowInTree",
                        icon: "icon-show-in-tree"
                    });
                }

                if (!_.findWhere(treeNode.actions, {name: "DiscardFromStash"})) {
                    treeNode.actions.push({
                        title: "Discard from Stash",
                        name: "DiscardFromStash",
                        icon: "icon-delete-content"
                    });
                }

                if (deleteAction) treeNode.actions.push(deleteAction);

            });
        };

        return treeNode;
    }

    _discardFromStash(node) {
        let jstree = this.jstree();

        let deletePoint = node;
        let parent = jstree.get_node(node.parent);
        while (parent.children.length === 1 && parent.id !== this.rootID) {
            deletePoint = parent;
            parent = jstree.get_node(parent.parent);
        }

        jstree.select_node(parent.id);
        jstree.delete_node([deletePoint.id]);

        this.discardedCount++;
    }

    exitStashState(options) {
        let artifact = options && options.target ? options.target.targetRepoKey || '/' : this.$stateParams.artifact || '';
        this.$state.go('artifacts.browsers.path', {tab: 'General', artifact: artifact, browser: 'tree'});
        this.$timeout(()=> {
            this.JFrogEventBus.dispatch(this.EVENTS.TREE_REFRESH);
            if (options) {
                this.JFrogEventBus.dispatch(this.EVENTS.ACTION_COPY, {node: options.node, target: options.target});
            }
        })
    }
}
import EVENTS from "../../../constants/artifacts_events.constants";
import KEYS from "../../../constants/keys.constants";
import ACTIONS from "../../../constants/artifacts_actions.constants";

const JSTREE_ROW_HOVER_CLASS = 'jstree-hovered';
const REGEXP = /(pkg|repo)\:\ *([^;]+)(?:;|$)\ */g;

export default class JFCommonBrowser {
    constructor(ArtifactActions, AdvancedStringMatch, ArtifactoryState, $timeout) {

        this.ArtifactoryState = ArtifactoryState;
        this.advancedStringMatch = AdvancedStringMatch;
        this.artifactActions = ArtifactActions;
        this.$timeout = $timeout;
        this.activeFilter = false;

        if (this.browserController) {
            this.activeFilter = this.browserController.activeFilter || false;
            this.searchText = this.browserController.searchText || '';
            if (this.searchText.endsWith('*')) this.searchText = this.searchText.substr(0,this.searchText.length-1);
        }
        if (!localStorage.favoritesRepos) localStorage.setItem('favoritesRepos', '{"favoritesRepos" : []}');
        this._initJSTreeSorting();

        this.packageTypesReplacingArchives = ['bower','npm','cocoapods','gitlfs','opkg','composer','pypi','vagrant','helm'];
    }

    /****************************
     * Context menu items
     ****************************/

    _getContextMenuItems(obj, cb) {
        let actionItems = {};
        if (obj.data) {
            let node = obj.data;
            node.load()
            .then(() => node.refreshWatchActions())
            .then(() => node.getDownloadPath())
            .then(() => {
                if (node.actions) {
                    node.actions.forEach((actionObj) => {
                        let name = actionObj.name;
                        let action = angular.copy(ACTIONS[name]);

                        if (name == 'Favorites' && localStorage.favoritesRepos) {
                            let favoritesRepos = JSON.parse(localStorage.favoritesRepos);
                            action.icon = _.contains(favoritesRepos.favoritesRepos, node.repoKey) ? 'icon-star-full' : 'icon-star';
                            action.title = _.contains(favoritesRepos.favoritesRepos, node.repoKey) ? 'Remove from Favorites' : 'Add to Favorites';
                        }

                        if (!action) {
                            console.log("Unrecognized action", name);
                            return true;
                        }
                        action._class = 'menu-item-' + action.icon;
                        action.icon = 'action-icon icon ' + action.icon;
                        action.label = action.title;

                        if (actionObj.name === 'Download') {
                            action.link = node.actualDownloadPath;
                        }
                        else {                        
                            action.action = () => {
                                this.artifactActions.perform(actionObj, obj, true);
                            }
                        }
                if (actionObj.name === 'Distribute' && node.repoType === 'distribution') {
                    action = angular.copy(ACTIONS['Redistribute']);
                    ;
                }
                        actionItems[name] = action;
                    });

                    cb(actionItems);
                }
                else {
                    cb([]);
                }
            });
        }
        else {
            cb([]);
        }
    }

    /****************************
     * Access methods
     ****************************/
    jstree() {
        return $(this.treeElement).jstree();
    }

    _getSelectedTreeNode() {
        let selectedJsNode = this.jstree().get_node(this._getSelectedNode());
        return selectedJsNode && selectedJsNode.data;
    }

    _getSelectedNode() {
        return this.jstree().get_selected()[0];
    }

    /****************************
     * access the tree
     ****************************/

    _isVisible(jsTreeNode) {
        // If the node is hidden, the get_node as DOM returns empty result
        return this.jstree().get_node(jsTreeNode, true).length && $('#'+this._getSafeId(jsTreeNode.id)).css('display') !== 'none';
    } 

    _isRootRepoVisible(jsTreeNode) {
        return this.jstree().get_node(this._getRootRepo(jsTreeNode), true).length;
    }

    _getFirstVisibleNode() {
        let json = this.jstree().get_json();
        for (let node of json) {
            if (this._isVisible(node)) {
                return node;
            }
        }
    }

    _getRootRepo(jsTreeNode) {
        return jsTreeNode.data && jsTreeNode.data.getRoot ? jsTreeNode.data.getRoot() : null;
    }

    _unhoverAll() {
        $('.' + JSTREE_ROW_HOVER_CLASS).removeClass(JSTREE_ROW_HOVER_CLASS);
    }

    _hover(domElement) {
        domElement.find('.jstree-anchor').first().addClass(JSTREE_ROW_HOVER_CLASS);
    }

    _focusOnTree(elemToFocus) {
        if (!elemToFocus) {
            var elemToFocus = ($('.jstree-hovered').length) ? $('.jstree-hovered').closest('.jstree-node').attr('id') : $('.jstree-clicked').closest('.jstree-node').attr('id');
        }

        $('.jstree #' + elemToFocus + '_anchor').focus();
    }

    _getSafeId(id) {
        return this._escapeChars(id,['/','.','$','{','}','(',')','[',']']);
    }

    _escapeChars(str,chars) {
        let newStr = str;
        chars.forEach((char)=>{
            newStr = newStr ? newStr.split(char).join('\\'+char) : newStr;
        });
        return newStr;
    }

    _getDomElement(node) {
        return this.jstree().get_node(node, true);
    }

    _scrollIntoView(domElement) {
        if (!domElement || !domElement[0]) return;

/*
        if (domElement[0].scrollIntoViewIfNeeded) {
            domElement[0].scrollIntoViewIfNeeded(true);
        }
        else {
*/
        this._scrollToViewIfNeededReplacement(domElement[0],true);
        if (domElement[0].scrollIntoViewIfNeeded) {
            domElement[0].scrollIntoViewIfNeeded(true);
        }
/*
        }
*/
    }

    _scrollToViewIfNeededReplacement(elem,centerIfNeeded,runAgain = true) {
        function withinBounds(value, min, max, extent) {
            if (false === centerIfNeeded || max <= value + extent && value <= min + extent) {
                return Math.min(max, Math.max(min, value));
            } else {
                return (min + max) / 2;
            }
        }

        function makeArea(left, top, width, height) {
            return  { "left": left, "top": top, "width": width, "height": height
                , "right": left + width, "bottom": top + height
                , "translate":
                    function (x, y) {
                        return makeArea(x + left, y + top, width, height);
                    }
                , "relativeFromTo":
                    function (lhs, rhs) {
                        var newLeft = left, newTop = top;
                        lhs = lhs.offsetParent;
                        rhs = rhs.offsetParent;
                        if (lhs === rhs) {
                            return area;
                        }
                        for (; lhs; lhs = lhs.offsetParent) {
                            newLeft += lhs.offsetLeft + lhs.clientLeft;
                            newTop += lhs.offsetTop + lhs.clientTop;
                        }
                        for (; rhs; rhs = rhs.offsetParent) {
                            newLeft -= rhs.offsetLeft + rhs.clientLeft;
                            newTop -= rhs.offsetTop + rhs.clientTop;
                        }
                        return makeArea(newLeft, newTop, width, height);
                    }
            };
        }

        var parent, area = makeArea(
            elem.offsetLeft, elem.offsetTop,
            elem.offsetWidth, elem.offsetHeight);
        while ((parent = elem.parentNode) instanceof HTMLElement) {
            var clientLeft = parent.offsetLeft + parent.clientLeft;
            var clientTop = parent.offsetTop + parent.clientTop;

            // Make area relative to parent's client area.
            area = area.
            relativeFromTo(elem, parent).
            translate(-clientLeft, -clientTop);

            parent.scrollLeft = withinBounds(
                parent.scrollLeft,
                area.right - parent.clientWidth, area.left,
                parent.clientWidth);

            parent.scrollTop = withinBounds(
                parent.scrollTop,
                area.bottom - parent.clientHeight, area.top,
                parent.clientHeight);

            // Determine actual scroll amount by reading back scroll properties.
            area = area.translate(clientLeft - parent.scrollLeft,
                clientTop - parent.scrollTop);
            elem = parent;
        }

        if (runAgain) this._scrollToViewIfNeededReplacement(elem,centerIfNeeded,false); //hackish fix

    }

    _getScoreObjectFromOrderArray(order) {
        let repoScore = {};
        let score = 100000;

        order.forEach((repoType) => {
            repoScore[repoType.toLowerCase()] = score;
            if (repoType === 'REMOTE') {
                repoScore['cached'] = score;
                score = score / 10;
            }
            score = score / 10;
        });
        return repoScore;
    }

    _initJSTreeSorting() {

        let jstree;
        let repoScore, repoOrder;
        $.jstree.defaults.sort = (a,b) => {
            if (!repoOrder) {
                repoOrder = this.ArtifactoryState.getState('repoOrder');
                repoScore = this._getScoreObjectFromOrderArray(repoOrder || ['VIRTUAL','DISTRIBUTION', 'LOCAL', 'REMOTE']);
            }

            if (!jstree) jstree = this.jstree();
            let aNode = jstree.get_node(a);
            let bNode = jstree.get_node(b);

            if (!aNode || !bNode) {
                jstree = this.jstree();
                aNode = jstree.get_node(a);
                bNode = jstree.get_node(b);
            }

            let aText = aNode.data ? aNode.data.text.toLowerCase() : '*';
            let bText = bNode.data ? bNode.data.text.toLowerCase() : '*';

            let aType = aNode.data ? aNode.data.type : '*';
            let bType = bNode.data ? bNode.data.type : '*';
            let aRepoType = aNode.data ? aNode.data.repoType : '*';
            let bRepoType = bNode.data ? bNode.data.repoType : '*';
            let aPkgType = aNode.data ? aNode.data.repoPkgType : '*';
            let bPkgType = bNode.data ? bNode.data.repoPkgType : '*';

            let aScore=0,bScore=0;

            if (aNode.data && aNode.data.isTrashcan && aNode.data.isTrashcan() && aNode.text !== '..') return 1;
            else if (bNode.data && bNode.data.isTrashcan && bNode.data.isTrashcan() && bNode.text !== '..') return -1;
            else if ((aType === 'repository' || aType === 'virtualRemoteRepository') &&
                (bType === 'repository' || bType === 'virtualRemoteRepository')) {
                //both repos - top level sort

                if (localStorage.treeSortingMethod  === 'pkg_type') {

                    if (aPkgType < bPkgType) aScore+=1000000;
                    if (bPkgType < aPkgType) bScore+=1000000;

                }

                if (localStorage.treeSortingMethod  === 'repo_type' || localStorage.treeSortingMethod  === 'pkg_type' || !localStorage.treeSortingMethod) {
                    if (aRepoType === 'distribution') aScore += repoScore.distribution;
                    if (bRepoType === 'distribution') bScore += repoScore.distribution;

                    if (aRepoType==='local') aScore+=repoScore.local;
                    if (bRepoType==='local') bScore+=repoScore.local;

                    if (aRepoType==='cached') aScore+=repoScore.cached;
                    if (bRepoType==='cached') bScore+=repoScore.cached;

                    if (aRepoType==='remote') aScore+=repoScore.remote;
                    if (bRepoType==='remote') bScore+=repoScore.remote;

                    if (aRepoType==='virtual') aScore+=repoScore.virtual;
                    if (bRepoType==='virtual') bScore+=repoScore.virtual;
                }
                else


                if (aText<bText) aScore++;
                if (aText>bText) bScore++;

                return aScore<bScore?1:-1;
            }
            else if ((aType !== 'repository' && aType !== 'virtualRemoteRepository') &&
                     (bType !== 'repository' && bType !== 'virtualRemoteRepository')) {
                //both files or folders

                if (aType==='folder') aScore+=10000;
                if (bType==='folder') bScore+=10000;

                if (aNode.text === '..') aScore+=100000;
                if (bNode.text === '..') aScore+=100000;

                let aHasNumVal = !_.isNaN(parseInt(aText));
                let bHasNumVal = !_.isNaN(parseInt(bText));

                if (aHasNumVal && bHasNumVal) {

                    let addTo = this._compareVersions(aText,bText);

                    if (addTo==='a') aScore += 100;
                    if (addTo==='b') bScore += 100;
                }
                else {

                    let aDigitIndex = aText.search(/\d/);
                    let bDigitIndex = bText.search(/\d/);

                    if (aDigitIndex === bDigitIndex && aDigitIndex !== -1) {
                        let aBeforeNum = aText.substr(0,aDigitIndex);
                        let bBeforeNum = bText.substr(0,bDigitIndex);
                        if (aBeforeNum === bBeforeNum) {
                            let aFromNum = aText.substr(aDigitIndex);
                            let bFromNum = bText.substr(bDigitIndex);

                            let addTo = this._compareVersions(aFromNum,bFromNum);

                            if (addTo==='a') aScore += 100;
                            if (addTo==='b') bScore += 100;

                        }
                    }

                    if (aText<bText) aScore++;
                    if (aText>bText) bScore++;
                }
                return aScore<bScore?1:-1;
            }
            else {
                if (!aNode.data) return -1; //special node
                else if (!bNode.data) return 1; //special node
                else if ((aType === 'repository' || aType === 'virtualRemoteRepository')) return -1;
                else if ((bType === 'repository' || bType === 'virtualRemoteRepository')) return 1;
                else return aText>bText?1:-1;
            }
        }
    }

    _compareVersions(aText,bText) {
        let splitters = /\-|\.|_/
        let aArr = aText.split(splitters);
        let bArr = bText.split(splitters);
        let minLength = Math.min(aArr.length,bArr.length);

        let addTo;
        for (let i = 0; i<minLength; i++) {
            let aNum = parseInt(aArr[i]);
            let bNum = parseInt(bArr[i]);
            let aIsNum = !_.isNaN(aNum);
            let bIsNum = !_.isNaN(bNum);
            if (aIsNum && bIsNum && aNum<bNum) {
                addTo = 'a';
                break;
            }
            else if (aIsNum && bIsNum && aNum>bNum) {
                addTo = 'b';
                break;
            }
            else if (!aIsNum || !bIsNum) {
                if (aArr[i]<bArr[i]) {
                    addTo = 'a';
                    break;
                }
                else if (aArr[i]>bArr[i]) {
                    addTo = 'b';
                    break;
                }
            }
        }

        if (!addTo) {
            if (aArr.length > bArr.length) addTo = 'b';
            else if (aArr.length < bArr.length) addTo = 'a';
        }

        return addTo;
    }

    _getFileExtension(path){
        return (path.indexOf('.') === -1 ? '' : path.split('.').pop());
    }

    _iconsShouldBeReplacedWithRepoPkgTypeIcon(nodeType,repoPkgType,fullPath){
        let repotype = repoPkgType && repoPkgType.toLocaleLowerCase();

        return ((nodeType === 'archive' &&
        ($.inArray(repotype,this.packageTypesReplacingArchives) !== -1))
        ||
        (repoPkgType === 'Gradle' &&
        this._getFileExtension(fullPath) === 'gradle'));
    }

    /************************************************************************************************************************************
     * New Advanced Quick Find !!
     ************************************************************************************************************************************/

    /********************************************
     * Is the node matching the search criteria
     ********************************************/

    _ensureVisibility(domElement) {
        if (!domElement.hasClass) return;
        this.$timeout(()=>{
            if (domElement.hasClass('hidden')) {
                let id = domElement.prop('id');
                let treeNode = this.jstree().get_node(id);
                let rootRepo = this._getRootRepo(treeNode);
            }
            this._filterTree();
        })
    }

    _shouldShowNode(tempFilterString, jsTreeNode) {
        if (jsTreeNode.id === '#' && jsTreeNode.parent === null && !jsTreeNode.data) return true;
        if ((!jsTreeNode.parent || jsTreeNode.parent === '#') && jsTreeNode.text === '..' && jsTreeNode.type === 'go_up') return true;
        if (!jsTreeNode.data) return false;
        let treeNode = jsTreeNode.data;

        let rootRepo = this._getRootRepo(jsTreeNode);

        let isFilterMatch = (str, filterText) => {
            if (filterText.indexOf(',') === -1) {
                return str.toLowerCase().trim().indexOf(filterText.toLowerCase().trim()) != -1;
            }
            else {
                let filterArray = filterText.split(',');
                return filterArray.reduce((acc,val)=>{return acc || (val && str.toLowerCase().trim().indexOf(val.trim().toLowerCase()) != -1)},false)
            }
        }

        let checkFavorites = (treeNode) => {
            if (!treeNode.isRepo || !treeNode.isRepo()) return true;
            if (!localStorage.filterFavorites || !localStorage.favoritesRepos) return true;
            let favoritesRepos = JSON.parse(localStorage.favoritesRepos);
            if ((treeNode.isTrashcan && treeNode.isTrashcan() && this.type === 'tree') || _.includes(favoritesRepos.favoritesRepos, treeNode.repoKey)) return true;
            else return false;
        }

        let filterRegexp = new RegExp(REGEXP);
        let combinedFilter = this.getCombinedFilter(tempFilterString);
        let matches = filterRegexp.exec(combinedFilter);
        if (matches) {
            let ret = true;
            while (matches) {
                if (matches[2].trim()) {
                    let filterType = matches[1].trim();
                    let filterText = matches[2].trim();
                    switch(filterType) {
                        case 'pkg':
                            ret = ret && (
                                    (treeNode.isRepo()
                                            && treeNode.repoPkgType
                                            && isFilterMatch(treeNode.repoPkgType, filterText)
                                    )
                                    ||
                                    (!treeNode.isRepo()
                                    && rootRepo.isRepo()
                                    && isFilterMatch(rootRepo.repoPkgType, filterText))
                                    ||
                                    (
                                            (
                                                    (treeNode.isTrashcan
                                                    && treeNode.isTrashcan())
                                                    ||
                                                    (treeNode.isInTrashcan
                                                    && treeNode.isInTrashcan())
                                            )
                                            && localStorage.pinnedTrash
                                            && this.type === 'tree'
                                    )
                            );
                            break;
                        case 'repo':
                            ret = ret && (
                                    (treeNode.isRepo()
                                    && isFilterMatch(treeNode.repoType, filterText))
                                    ||
                                    (!treeNode.isRepo()
                                    && rootRepo.isRepo()
                                    && isFilterMatch(rootRepo.repoType, filterText))
                                    ||
                                    (
                                            (
                                                    (treeNode.isTrashcan
                                                    && treeNode.isTrashcan())
                                                    ||
                                                    (treeNode.isInTrashcan
                                                    && treeNode.isInTrashcan())
                                            )
                                            &&
                                            localStorage.pinnedTrash
                                            && this.type === 'tree'
                                    )
                            );
                            break;
                    }
                }
                matches = filterRegexp.exec(this.getCombinedFilter(tempFilterString));
            }
            return ret && checkFavorites(treeNode);
        }
        else return checkFavorites(treeNode);


    }

    /****************************
     * Searching the tree
     ****************************/
    _searchTree(text,gotoFirst = true, showSpinner = true) {
        this.searchText = text || '';
        this._filterTree();
        this._jsQuickFind(this.searchText,gotoFirst,showSpinner);
    }

    _cancelRunningSearch() {
        if (this.searchTimeoutPromise) {
            this.$timeout.cancel(this.searchTimeoutPromise);
            this.searchTimeoutPromise = null;
            this.JFrogEventBus.dispatch(EVENTS.TREE_SEARCH_RUNNING, false);
//            console.log('Quick Find Ended.')
        }
    }

    _filterTree(tempFilterString) {

        tempFilterString = tempFilterString || this.searchText;

        let allElements = $('.jstree-anchor');
        let elemsToShow = [];
        let elemsToHide = [];

        let filterHasNoMatches = true;

        for (let i = 0; i < allElements.length; i++) {
            let e = $(allElements[i]);
            if ((tempFilterString || this.getPersistentFilter()) && e.text()) {
                let id = e.prop('id');
                id = id.substr(0, id.length - '_anchor'.length);
                let treeNode = this.jstree().get_node(id);
                if (this._shouldShowNode(tempFilterString, treeNode, e[0])) {
                    elemsToShow.push(e[0].parentElement);
                    if (treeNode.type !== 'trashcan') filterHasNoMatches = false;
                }
                else {
                    elemsToHide.push(e[0].parentElement);
                }
            }
            else {
                elemsToHide.push(e[0].parentElement);
            }
        }


        if (elemsToShow.length || this.browserController.isFiltersOn() || (localStorage.filterFavorites && localStorage.favoritesRepos)) {
            this.browserController.filterHasNoMatches = filterHasNoMatches;
            $('.hidden:has(".jstree-anchor")').removeClass('hidden');
            $(elemsToShow).removeClass('hidden');
            $(elemsToHide).addClass('hidden');
        }
        else {
            $('.hidden:has(".jstree-anchor")').removeClass('hidden');
        }

    }
    _jsQuickFind(searchText, gotoFirst = true, showSpinner = true) {
        this._cancelRunningSearch();
        if (!searchText) {
            this._clear_search();
            return;
        }

        const CHUNK_SIZE = 500;

        let res = [];
        let nodes = [];
        let nodesParents = [];
        let nomatches = [];
        let nomatchesParents = [];
        let all = $('.jstree-anchor');

        let i = 0;
        let doIteration = () => {
//            console.log('Quick Find Running...')
            let startI = i;
            while (i < startI + CHUNK_SIZE && i<all.length) {
                let e = $(all[i]);
                if ((searchText || this.getPersistentFilter()) && e.text()) {
                    let id = e.prop('id');
                    id = id.substr(0, id.length - '_anchor'.length);
                    let treeNode = this.jstree().get_node(id);

                    let matchObj = null;
                    if (treeNode.data && this._isVisible(treeNode)) {
                        matchObj = this.advancedStringMatch.match(this.type === 'stash' ? treeNode.text : treeNode.data.isTrashcan() ? "Trash Can" : treeNode.data.text,searchText);
                        if (e[0]) {
                            if (matchObj.matched) $(e[0]).prop('segments',matchObj.segments);
                            else $(e[0]).prop('segments',null);
                        }
                    }


                    if (matchObj && matchObj.matched) {
                        let goneToFirst = false;
                        if (gotoFirst && !nodes.length) { //goto first result
                            goneToFirst = true;
                            this.searchResults = [id];
                            this._initCurrentSearchResult([e[0]]);
                            this._gotoCurrentSearchResult();
                        }
                        res.push(id);
                        nodes.push(e[0]);
                        nodesParents.push(e[0].parentElement);


                        if (goneToFirst || this._isScrolledIntoView(e[0], 0)) {
                            e.addClass('jstree-search');
                            let jqe = $(e[0]);
                            jqe.unhighlight();
                            let segs = jqe.prop('segments');
                            if (segs) this.advancedStringMatch.highlight(jqe,segs);
                        }
                    }
                    else {
                        nomatches.push(e[0]);
                        nomatchesParents.push(e[0].parentElement);
                        if (this._isScrolledIntoView(e[0], 0)) {
                            e.removeClass('jstree-search');
                            $(e[0]).unhighlight();
                        }
                    }
                }
                else {
                    nomatches.push(e[0]);
                    nomatchesParents.push(e[0].parentElement);
                    if (this._isScrolledIntoView(e[0], 0)) {
                        e.removeClass('jstree-search');
                        $(e[0]).unhighlight();
                    }
                }
                i++;
                this.searchResults = res;
                this.searchNodes = nodes;
                this.searchParentNodes = nodes;
                this.nomatchNodes = nomatches;
                this.nomatchParentNodes = nomatchesParents;
            }

        };

        let timeOutFunc = () => {
            if (i < all.length) {
                if (showSpinner && i === 0 && all.length > 250) this.JFrogEventBus.dispatch(EVENTS.TREE_SEARCH_RUNNING, true);
                doIteration();
                this.searchTimeoutPromise = this.$timeout(() => timeOutFunc());
            }
            else { // finish

                this.searchTimeoutPromise = null;

                this.JFrogEventBus.dispatch(EVENTS.TREE_SEARCH_RUNNING, false);
//                console.log('Quick Find Ended.')
            }
        };
// console.log('Starting Quick Find...')
        this.searchTimeoutPromise = this.$timeout(() => timeOutFunc());

    }

    _onScroll() {
        if (!this.searchNodes) return;
        for (let i = 0; i<this.searchNodes.length; i++) {
            if (this._isScrolledIntoView(this.searchNodes[i], 0)) {
                let jqe = $(this.searchNodes[i]);
                jqe.addClass('jstree-search');
                jqe.unhighlight();
                let segs = jqe.prop('segments');
                if (segs) this.advancedStringMatch.highlight(jqe,segs);
            }
        }
        for (let i = 0; i<this.nomatchNodes.length; i++) {
            if (this._isScrolledIntoView(this.nomatchNodes[i], 0)) {
                let jqe = $(this.nomatchNodes[i]);
                jqe.removeClass('jstree-search');
                jqe.unhighlight();
            }
        }
    }


    _isScrolledIntoView(el, marginSize = 0) {
        let elemTop = el.getBoundingClientRect().top;
        let elemBottom = el.getBoundingClientRect().bottom;

        var isVisible = (elemTop >= -marginSize) && (elemBottom <= window.innerHeight + marginSize);
        return isVisible;
    }

    _initCurrentSearchResult(nodes) {
        if (!this.currentResult || !_.include(this.searchResults, this.currentResult)) {
            // there is no previous result, or previous result is not included in the search results
            // select first result that's below the node we started the search from
            let startFromDom = this.jstree().get_node(this._getSelectedNode(), /* as_dom = */ true)[0];
            let firstNodeBelow = _.find(nodes, (node) => {
                if (!startFromDom) return true;
                return node.offsetTop > startFromDom.offsetTop;
            });
            // if found - select as first result, if not - select first search result
            this.currentResult = firstNodeBelow ? firstNodeBelow.id.substr(0,firstNodeBelow.id.length - '_anchor'.length) : this.searchResults[0];
        }
    }

    _isInActiveFilterMode(checkIfMatchesFound = false) {
        if (this.getPersistentFilter()) return true;
        if (this.searchText.match(new RegExp(REGEXP))) {
            let justSearchTerm = this.searchText.substr(this.searchText.indexOf(':')+1).trim();
            if (justSearchTerm) {
                if (checkIfMatchesFound) {
                    let json = this.jstree().get_json();
                    let matchesFound = false;
                    for (let node of json) {
                        node.data.isRepo = () => {
                            return node.data.type === 'repository' ||
                                node.data.type === 'virtualRemoteRepository' ||
                                node.data.type === 'localRepository' ||
                                node.data.type === 'remoteRepository' ||
                                node.data.type === 'cachedRepository' ||
                                node.data.type === 'virtualRepository';
                        };
                        if (this._shouldShowNode(this.searchText,node)) {
                            matchesFound = true;
                            break;
                        }
                    }
                    return matchesFound ? true : 'no results';
                }
                else {
                    return true;
                }
            }
            else return 'empty';
        }
        else return !!this.searchText.trim();
    }

    _searchTreeKeyDown(key) {
        let jstree = this.jstree();
        if (key == KEYS.DOWN_ARROW) {
            this._selectNextSearchResult();
        }
        else if (key == KEYS.UP_ARROW) {
            this._selectPreviousSearchResult();
        }
        else if (key == KEYS.ENTER) {
            //manually set the model to the input element's value (because the model is debounced...)
            this.searchText = $('.jf-tree-search').val();

            let isInActiveFilterMode = this._isInActiveFilterMode(true);

            if (isInActiveFilterMode === true) {
                this.activeFilter = true;
                if (this.browserController) {
                    this.browserController.activeFilter = true;
                    this.browserController.searchText = this.searchText + '*';
                }
                this._searchTree(this.searchText);
                this._focusOnTree();
                if (!this._isVisible(jstree.get_node(this._getSelectedNode()))) {
                    jstree.select_node(this._getFirstVisibleNode());
                }
            }
            else if (isInActiveFilterMode === 'no results') {
                if (this.artifactoryNotifications) this.artifactoryNotifications.create({warn: "No repositories matches the filtered " + (this.searchText.startsWith('pkg:') ? 'package' : 'repository') + " type"});
            }
            else {
                this.activeFilter = false;
                if (this.browserController) this.browserController.activeFilter = false;
                this._selectCurrentSearchResult();
                jstree.open_node(this.currentResult);
                this._clear_search();
                this._focusOnTree();
                this.currentResult = null;
            }
        }
        else if (key == KEYS.ESC) {
            this.activeFilter = false;
            if (this.browserController) this.browserController.activeFilter = false;
            this._clear_search();
            this._focusOnTree();
            this.currentResult = null;
        }
        else {
            this.$timeout(()=>{
                this.searchText = $('.jf-tree-search').val();
                if (this.searchText === '') {
                    $(this.treeElement).unhighlight();
                }
            })
        }
    }

    _clear_search() {
        this._cancelRunningSearch();
        this._unhoverAll();
        if (this.jstree().clear_search) this.jstree().clear_search();
        this.activeFilter = false;
        if (this.browserController) this.browserController.activeFilter = false;
        $('.jstree-anchor.jstree-search').removeClass('jstree-search');
        $(this.treeElement).unhighlight();
        this.searchNodes = null;
        this.searchParentNodes = null;
        this.nomatchNodes = null;
        this.nomatchParentNodes = null;
        this.searchText = '';
    }
    _clear_filter() {
        $('.hidden:has(".jstree-anchor")').removeClass('hidden');
    }

    _edit_search() {
        this.activeFilter = false;
        if (this.browserController) this.browserController.activeFilter = false;
        this.JFrogEventBus.dispatch(EVENTS.ACTIVATE_TREE_SEARCH,this.searchText);

    }

    _selectNextSearchResult() {
        let index = this.searchResults.indexOf(this.currentResult);
        index++;
        if (index > this.searchResults.length - 1) {
            index = 0;
        }
        this.currentResult = this.searchResults[index];
        this._gotoCurrentSearchResult();
    }

    _selectPreviousSearchResult() {
        let index = this.searchResults.indexOf(this.currentResult);
        index--;
        if (index < 0) {
            index = this.searchResults.length - 1;
        }
        this.currentResult = this.searchResults[index];
        this._gotoCurrentSearchResult();
    }

    _gotoCurrentSearchResult() {
        this._unhoverAll();
        if (this.currentResult) {
            let domElement = this._getDomElement(this.currentResult);
            this._hover(domElement);
            this._scrollIntoView(domElement);
        }
    }

    _selectCurrentSearchResult() {
        if (this.currentResult) {
            this.jstree().deselect_all();
            this.jstree().select_node(this.currentResult);
        }
    }

    getPersistentFilter() {
        let persistentFilter = localStorage.getItem("treeFiltering");
        if (persistentFilter) {
            persistentFilter = JSON.parse(persistentFilter);
            let filterArray = [];
            if (persistentFilter.pkg.length) filterArray.push(`pkg:${persistentFilter.pkg.join(',')}`);
            if (persistentFilter.repo.length) filterArray.push(`repo:${persistentFilter.repo.join(',')}`);
            let filterString = filterArray.join(';');

            return filterString || (localStorage.filterFavorites && localStorage.favoritesRepos ? '*' : '');
        }
        return localStorage.filterFavorites && localStorage.favoritesRepos ? true : null;
    }

    getCombinedFilter(tempFilter) {
        let persistentFilter = this.getPersistentFilter();
        if (!tempFilter) {
            return persistentFilter;
        }
        else {

            let persistentObj = this._filterStringToFilterObject(persistentFilter);
            let tempObj = this._filterStringToFilterObject(tempFilter);

            let pkgArray = persistentObj.pkg ? _.filter(persistentObj.pkg,val=>{
                return _.find(tempObj.pkg,tempVal=>val.toLowerCase().indexOf(tempVal) !== - 1);
            }) : tempObj.pkg || [];
            let repoArray = persistentObj.repo ? _.filter(persistentObj.repo,val=>{
                return _.find(tempObj.repo,tempVal=>val.toLowerCase().indexOf(tempVal) !== - 1);
            }) : tempObj.repo || [];

            if (!pkgArray.length) pkgArray = persistentObj.pkg;
            if (!repoArray.length) repoArray = persistentObj.repo;

            let combinedArray = [];
            if (pkgArray && pkgArray.length) combinedArray.push(`pkg:${pkgArray.join(',')}`);
            if (repoArray && repoArray.length) combinedArray.push(`repo:${repoArray.join(',')}`);
            let combinedString = combinedArray.join(';');

            return combinedString;
        }
    }

    _filterStringToFilterObject(filterString) {

        if (!filterString || !_.isString(filterString)) return {};

        let filterRegexp = new RegExp(REGEXP);

        let part1 = filterRegexp.exec((filterString.match(filterRegexp) || [])[0]) || [];
        let part2 = filterRegexp.exec((filterString.match(filterRegexp) || [])[1]) || [];

        let obj = {
            [part1[1]]: (part1[2] || '').split(','),
            [part2[1]]: (part2[2] || '').split(',')
        }

        delete obj.undefined;
        return obj;

    }

}
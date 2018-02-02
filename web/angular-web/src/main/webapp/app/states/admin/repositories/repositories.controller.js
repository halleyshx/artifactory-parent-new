import FIELD_OPTIONS from "../../../constants/field_options.constats";
import CONFIG_MESSAGES from "../../../constants/configuration_messages.constants";
import EVENTS from "../../../constants/artifacts_events.constants";

export class AdminRepositoriesController {

    constructor($scope, $state, JFrogGridFactory, RepositoriesDao, JFrogModal, uiGridConstants,
            ArtifactActionsDao, ArtifactoryFeatures, commonGridColumns, GlobalReplicationsConfigDao, JFrogEventBus) {
        this.artifactoryGridFactory = JFrogGridFactory;
        this.$state = $state;
        this.commonGridColumns = commonGridColumns;
        this.repositoriesDao = RepositoriesDao;
        this.globalReplicationsConfigDao = GlobalReplicationsConfigDao;
        this.$scope = $scope;
        this.modal = JFrogModal;
        this.artifactActionsDao = ArtifactActionsDao;
        this.gridOption = {};
        this.uiGridConstants = uiGridConstants;
        this.CONFIG_MESSAGES = CONFIG_MESSAGES.admin.repositories;
        this.features = ArtifactoryFeatures;
        this.currentRepoType = $state.params.repoType;
        this.gridData = {};
        this.JFrogEventBus = JFrogEventBus;
        if (!_.contains(['local','remote','virtual', 'distribution'], this.currentRepoType)) {
            this.$state.go('not_found_404');
            return;
        }

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.REFRESH_PAGE_CONTENT,()=>{
            this._initRepos();
        });

        this._createGrid();
        this._initRepos();
        this._getGlobalReplicationsStatus();
    }

    isCurrentRepoType(type) {
        return this.currentRepoType == type;
    }

    /**
     * Creates the grid according to current repo type, sets draggable according to the global repo status
     * NOTE: Multi select and batch actions are commented until batch delete repos is approved for prod.
     */
    _createGrid() {
        this.gridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
            .setColumns(this._getColumns())
            .setSingleSelect();
            //.setBatchActions(this._getBatchActions())
        this.gridOption.setRowTemplate('default');
    }

    _initRepos() {
        this.repositoriesDao.getRepositories({type: this.currentRepoType}).$promise
                .then((data) => {
                    this.noReposMessage = this.CONFIG_MESSAGES[this.currentRepoType].noReposMessage;
                    _.forEach(data, (row) => {
                        var rowPackageType =_.find(FIELD_OPTIONS.repoPackageTypes, (type) => {
                            return type.value == row.repoType.toLowerCase();
                        });
                        if (rowPackageType) {
                            row.displayType = rowPackageType.text;
                            row.typeIcon = rowPackageType.icon;
                        } else if (row.repoType.toLowerCase() === FIELD_OPTIONS.REPO_TYPE.DISTRIBUTION) {

                            /*this.repositoriesDao.checkBintrayAuthentication({repoKey: row.repoKey}).$promise.then((result,row) => {
                                //row.status = result;
                                console.log('Check validation for: ' + row.repoKey);
                            }).catch(() => {
                                row.status = "Error";
                                console.log('Error checking ' + row.repoKey);
                            });*/

                        }
                        else row.ignore = true;
                    });
                    data = _.filter(data, (row) => !row.ignore);
                    this.gridData = data;
                    this.gridOption.setGridData(data);
                });
    }

    reorderRepositories() {
        return this.repositoriesDao.reorderRepositories({repoType: this.currentRepoType}, this.getRepoOrder()).$promise
    }

    getRepoOrder() {
        let repoOrderList = [];
        this.gridData.forEach((data)=> {
            repoOrderList.push(data.repoKey);
        });
        return repoOrderList;
    }

    _deleteSelected(row) {
        this.modal.confirm("Are you sure you wish to delete this repository? All artifacts will be permanently deleted.", 'Delete ' + row.repoKey + " Repository", {confirm: 'Delete'})
                .then(()=> {
                    this.repositoriesDao.deleteRepository({
                        type: this.currentRepoType,
                        repoKey: row.repoKey
                    }).$promise.then((result)=> {
                                this._initRepos();
                            })
                });
    }

    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this._deleteSelectedRepos()
            }
        ]
    }

    _deleteSelectedRepos() {
        let selectedRows = this.gridOption.api.selection.getSelectedGridRows();
    }

    _editSelected(row) {
        this.$state.go('^.list.edit', {repoType: this.currentRepoType, repoKey: row.repoKey});
    }


    createNewRepo() {
        this.$state.go('^.list.new', {repoType: this.currentRepoType});
    }

    _calculateIndex(row) {
        this.artifactActionsDao.perform({
            action: 'calculateIndex',
            type: row.repoType,
            repoKey: row.repoKey
        })
    }

    localReplicationsRunNow(repoKey) {
        this.repositoriesDao.runNowReplications({repoKey: repoKey}).$promise.then(()=> {
        });
    }

    remoteExecuteReplicationNow(repoKey) {
        this.repositoriesDao.executeRemoteReplicationNow({repoKey: repoKey},
                this.repoInfo).$promise.then((result)=> {

                });
    }

    _getGlobalReplicationsStatus() {
        this.globalReplicationsConfigDao.status().$promise.then((status) => {
            this.globalReplicationsStatus = {
                blockPullReplications: status.blockPullReplications,
                blockPushReplications: status.blockPushReplications
            }
        });
    }


    _getColumns() {
        switch(this.currentRepoType) {
            case 'local':
                return this._getLocalColumns();
            case 'remote':
                return this._getRemoteColumns();
            case 'virtual':
                return this._getVirtualColumns();
            case 'distribution':
                return this._getDistColumns();
        }
    }

    _getLocalColumns() {
        return [
            {
                name: 'Repository Key',
                displayName: 'Repository Key',
                field: 'repoKey',
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.list.edit({repoType:\'local\',repoKey: row.entity.repoKey})" id="repositories-local-key">{{COL_FIELD}}</a></div>',
                width: '55%',
                enableSorting: true
                //sort: {
                //    direction: this.uiGridConstants.ASC
                //}
            },
            {
                name: 'Type',
                displayName: 'Type',
                field: 'displayType',
                cellTemplate: this.commonGridColumns.iconColumn('row.entity.displayType', 'row.entity.typeIcon', 'repo-type-icon'),
                width: '15%',
                enableSorting: true
            },
            {
                name: 'Recalculate Index',
                displayName: 'Recalculate Index',
                field: 'reindex',
                cellTemplate: '<div class="ui-grid-cell-contents text-center"><a class="grid-column-button icon icon-re-index" ng-click="!row.entity.hasReindexAction || grid.appScope.Repositories._calculateIndex(row.entity)" ng-disabled="!row.entity.hasReindexAction" jf-tooltip="{{row.entity.hasReindexAction ? \'Recalculate Index Now\' : \'Recalculate Not Supported For Repo Type\'}}" id="repositories-local-reindex"></a></div>',
                width: '15%',
                enableSorting: true
            },
            {
                name: 'Replications',
                displayName: 'Replications',
                field: 'replications',
                cellTemplate: '<div class="ui-grid-cell-contents text-center" ng-class="{\'replication-disabled\': grid.appScope.Repositories.globalReplicationsStatus.blockPushReplications}"><a class="grid-column-button icon icon-run" ng-click="(!row.entity.replications || grid.appScope.Repositories.globalReplicationsStatus.blockPushReplications) || grid.appScope.Repositories.localReplicationsRunNow(row.entity.repoKey)" ng-disabled="!row.entity.replications" jf-tooltip="{{grid.appScope.Repositories.globalReplicationsStatus.blockPushReplications ? \'Push Replication Is Blocked\' : (row.entity.replications ? \'Run Replication\' : \'No Replication Configured\')}}" id="repositories-local-replicate"></a></div>',
                width: '15%',
                actions: {
                    delete: row => this._deleteSelected(row)
                },
                enableSorting: false
            }
        ]
    }

    _getRemoteColumns() {
        return [
            {
                name: 'Repository Key',
                displayName: 'Repository Key',
                field: 'repoKey',
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.list.edit({repoType:\'remote\',repoKey: row.entity.repoKey})" id="repositories-remote-key">{{COL_FIELD}}</a></div>',
                width: '20%',
                enableSorting: true
            },
            {
                name: 'Type',
                displayName: 'Type',
                field: 'displayType',
                cellTemplate: this.commonGridColumns.iconColumn('row.entity.displayType', 'row.entity.typeIcon', 'repo-type-icon'),
                width: '10%',
                enableSorting: true
            },
            {
                name: 'URL',
                displayName: 'URL',
                field: 'url',
                width: '40%',
                enableSorting: true
            },
            {
                name: 'Recalculate Index',
                displayName: 'Recalculate Index',
                field: 'reindex',
                cellTemplate: '<div class="ui-grid-cell-contents text-center"><a class="grid-column-button icon icon-re-index" ng-click="!row.entity.hasReindexAction || grid.appScope.Repositories._calculateIndex(row.entity)" ng-disabled="!row.entity.hasReindexAction" jf-tooltip="{{row.entity.hasReindexAction ? \'Recalculate Index Now\' : \'Recalculate Not Supported For Repo Type\'}}" id="repositories-local-reindex"></a></div>',
                width: '15%',
                enableSorting: false
            },
            {
                name: 'Replications',
                displayName: 'Replications',
                field: 'hasEnabledReplication',
                cellTemplate: '<div class="ui-grid-cell-contents text-center" ng-class="{\'replication-disabled\': grid.appScope.Repositories.globalReplicationsStatus.blockPullReplications}"><a class="grid-column-button icon icon-run" ng-click="(!row.entity.hasEnabledReplication || grid.appScope.Repositories.globalReplicationsStatus.blockPullReplications) || grid.appScope.Repositories.remoteExecuteReplicationNow(row.entity.repoKey)" ng-disabled="!row.entity.hasEnabledReplication" jf-tooltip="{{grid.appScope.Repositories.globalReplicationsStatus.blockPullReplications ? \'Pull Replication Is Blocked\' : (row.entity.hasEnabledReplication ? \'Run Replication\' : \'No Replication Configured\')}}" id="repositories-local-replicate"></a></div>',
                width: '15%',
                actions: {
                    delete: row => this._deleteSelected(row)
                },
                enableSorting: false
            }
        ]
    }

    _getVirtualColumns() {
        return [
            {
                name: 'Repository Key',
                displayName: 'Repository Key',
                field: 'repoKey',
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.list.edit({repoType:\'virtual\',repoKey: row.entity.repoKey})" id="repositories-virtual-key">{{COL_FIELD}}</a></div>',
                width: '20%',
                enableSorting: true
            },
            {
                name: 'Type',
                displayName: 'Type',
                field: 'displayType',
                cellTemplate: this.commonGridColumns.iconColumn('row.entity.displayType', 'row.entity.typeIcon', 'repo-type-icon'),
                width: '10%',
                enableSorting: true
            },
            {
                name: 'Included Repositories',
                displayName: 'Included Repositories',
                field: 'numberOfIncludesRepositories',
                width: '15%',
                enableSorting: false
            },
            {
                name: 'Selected Repositories',
                displayName: 'Selected Repositories',
                field: 'selectedRepos',
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.selectedRepos','row.entity.repoKey', null, null, "repositories-virtual-selected"),
                width: '40%',
                enableSorting: false
            },
            {
                name: 'Recalculate Index',
                displayName: 'Recalculate Index',
                field: 'reindex',
                cellTemplate: '<div class="ui-grid-cell-contents text-center"><a class="grid-column-button icon icon-re-index" ng-click="!row.entity.hasReindexAction || grid.appScope.Repositories._calculateIndex(row.entity)" ng-disabled="!row.entity.hasReindexAction" jf-tooltip="{{row.entity.hasReindexAction ? \'Recalculate Index Now\' : \'Recalculate Not Supported For Repo Type\'}}" id="repositories-virtual-reindex"></a></div>',
                width: '15%',
                actions: {
                    delete: row => this._deleteSelected(row)
                },
                enableSorting: false
            }
        ];
    }

    _getDistColumns() {
        return [
            {
                name: 'Repository Key',
                displayName: 'Repository Key',
                field: 'repoKey',
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.list.edit({repoType:\'distribution\',repoKey: row.entity.repoKey})" id="repositories-distribution-key">{{COL_FIELD}}</a></div>',
                width: '85%',
                enableSorting: true
            },
            {
                name: 'Repository Visibility',
                displayName: 'Repository Visibility',
                field: 'visibility',
                cellTemplate: '<div class="ui-grid-cell-contents" id="repository-visibility">{{row.entity.visibility}}</div>',
                width: '15%',
                enableSorting: true,
                actions: {
                    delete: row => this._deleteSelected(row)
                }
            }/*,
            {
                name: 'Status',
                displayName: 'Status',
                field: 'status',
                cellTemplate: this.commonGridColumns.ajaxColumn(),
                width: '15%',
                actions: {
                    delete: row => this._deleteSelected(row);
                }
            }*/
        ];
    }
}
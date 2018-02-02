import TOOLTIP from "../../../../constants/artifact_tooltip.constant";
import EVENTS from "../../../../constants/artifacts_events.constants";

export class AdminSecurityPermissionsFormController {
    constructor($scope, $state, $stateParams, $q, JFrogModal, JFrogGridFactory, RepoDataDao,
            PermissionsDao, commonGridColumns, User, ArtifactoryModelSaver, JFrogEventBus) {
        this.$scope = $scope;
        this.$q = $q;
        this.repoDataDao = RepoDataDao;
        this.commonGridColumns = commonGridColumns;
        this.user = User.getCurrent();
        this.modal = JFrogModal;
        this.currentTab = 'repo';
        this.$state = $state;
        this.title = "New Permission";
        this.$stateParams = $stateParams;
        this.permission = {};
        this.permissionsDao = PermissionsDao.getInstance();
        this.newPermission = false;
        this.groupsGrid = [];
        this.usersGrid = [];
        this.selectedItems = [];
        this.artifactoryGridFactory = JFrogGridFactory;
        this.groupsGridOption = {};
        this.usersGridOption = {};
        this.TOOLTIP = TOOLTIP.admin.security.permissionsForm;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['permission']);
        this._createGroupsGrid();
        this._createUsersGrid();
        this.ArtifactoryModelSaver.save();
        this.JFrogEventBus = JFrogEventBus;

        this.dndComm = {};

        this.initPermissionForm();

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.REFRESH_PAGE_CONTENT, () => {
            this._getAllRepos().then(() => {
                this.allRepos.forEach((repo) => {
                    if (!_.find(this.permission.availableRepoKeys, {repoKey: repo.repoKey})
                            && !_.find(this.permission.repoKeys, {repoKey: repo.repoKey})) {
                        if (repo.type == 'local' && this.permission.anyLocal ||
                                repo.type == 'remote' && this.permission.anyRemote ||
                                repo.type == 'distribution' && this.permission.anyDistribution) {
                            repo["__fixed__"] = true;
                            this.permission.repoKeys.push(this.getRepoWithIcon(repo));
                        } else {
                            this.permission.availableRepoKeys.push(this.getRepoWithIcon(repo));
                        }
                    }
                });
                this.ArtifactoryModelSaver.save();
            });
        });

    }

    initPermissionForm() {
        if (this.$stateParams.permission) {
            this.initUpdatePermissionForm(this.$stateParams.permission);
            this.title = "Edit " + this.$stateParams.permission + ' Permission';
            this.newPermission = false;
        }
        else {
            this.newPermission = true;
            this.title = "New Permission";
            this._initNewPermissionForm();
        }
    }

    _getUsersAndGroups() {
        return this.permissionsDao.getAllUsersAndGroups().$promise.then((response) => {
            this.allUsersData = response.allUsers;
            this.allGroupsData = response.allGroups;
            this._filterAvailableUsers();
            this._filterAvailableGroups();
            this.ArtifactoryModelSaver.save();
        });
    }

    formInvalid() {
        return this.savePending || this.form.$invalid || !this.permission.repoKeys || !this.permission.repoKeys.length;
    }

    _getAllRepos() {
        let deferred = this.$q.defer();

        this.repoDataDao.getAllForPerms({"permission": true}).$promise.then((result) => {
            this.allRepos = result.repoTypesList;
            deferred.resolve();
        });
        return deferred.promise;

    }

    _initNewPermissionForm() {

        this.permission.anyLocal = false;
        this.permission.anyRemote = false;

        this.permission.includePatternArray = ['**'];
        this.permission.excludePatternArray = [];

        this.permission.availableRepoKeys = [];
        this.permission.repoKeys = [];

        this._getUsersAndGroups();
        this._getAllRepos().then(() => {
            this.permission.availableRepoKeys = _.map(this.allRepos, (repo) => {
                return this.getRepoWithIcon(repo);
            });
            this.ArtifactoryModelSaver.save();
        });
    }

    getRepoWithIcon(repo) {
        repo._iconClass = "icon " + (repo.type === 'local' ? "icon-local-repo" :
                        (repo.type === 'remote' ? "icon-remote-repo" : (repo.type === 'virtual' ? "icon-virtual-repo" :
                                        (repo.type === 'distribution' ? "icon-distribution-repo" :
                                                "icon-notif-error"))));
        return repo;
    }

    initUpdatePermissionForm(permission) {

        this.permissionsDao.getPermission({name: permission}).$promise.then((result) => {

            //console.log(result);
            this.permission = result;

            this.permission.repoKeys = _.map(this.permission.repoKeys, (repo) => {
                repo["__fixed__"] = (repo.type === 'local' && this.permission.anyLocal) || (repo.type === 'remote' && this.permission.anyRemote) || (repo.type === 'distribution' && this.permission.anyDistribution);
                return this.getRepoWithIcon(repo);
            });

            this.permission.availableRepoKeys = _.map(this.permission.availableRepoKeys, (repo) => {
                return this.getRepoWithIcon(repo);
            });

            this._getUsersAndGroups().then(()=>{

                // Build a map of all users as: user => isAdmin
                let userAdminMap = this.getEntityGroupMap(this.allUsersData);
                // Get the users with admin icons and tooltips
                result.users = this.getEntitiyGroupPermissionsList('user',result.users,userAdminMap);

                // Build a map of all groups as: group => is admin
                let groupAdminMap = this.getEntityGroupMap(this.allGroupsData);
                // Get the groups with admin icons and tooltips
                result.groups = this.getEntitiyGroupPermissionsList('group',result.groups,groupAdminMap);

                this.usersGridOption.setGridData(result.users);
                this.groupsGridOption.setGridData(result.groups);

                this.permission.includePatternArray = this.permission.includePattern ?
                        this.permission.includePattern.split(',') : [];
                this.permission.excludePatternArray = this.permission.excludePattern ?
                        this.permission.excludePattern.split(',') : [];

                this.ArtifactoryModelSaver.save();
            });
        });
    }

    /**check and set current tab**/

    setCurrentTab(tab) {
        this.currentTab = tab;
    }

    isCurrentTab(tab) {
        return this.currentTab === tab;
    }

    /**
     * button pre and  forwd at the bottom page**/
    prevStep() {
        if (this.currentTab == 'groups') {
            this.setCurrentTab('repo');
            return;
        }
        if (this.currentTab == 'users') {
            this.setCurrentTab('groups');
            return;
        }
    }

    fwdStep() {
        if (this.currentTab == 'repo') {
            this.setCurrentTab('groups');
            return;
        }
        if (this.currentTab == 'groups') {
            this.setCurrentTab('users');
            return;
        }
    }


    addGroup(group) {
        if(group.icon_class && group.icon_class.indexOf('admin') >= 0 ) return;

        if (!this.permission.groups) {
            this.permission.groups = [];
        }
        this.permission.groups.push(group);
        this.groupsGridOption.setGridData(this.permission.groups);
        this._filterAvailableGroups();

        /*
         if (group.name) {
         this.groups = _.remove(this.permission.groups, {name: group.name});
         this.groupsGrid.push({principal: group.name});
         }
         else {
         this.groups = _.remove(this.permission.groups, group);
         this.groupsGrid.push(group);
         }

         this.groupsGridOption.setGridData(this.groupsGrid);
         */

    }

    addUser(user) {
        if(user.icon_class && user.icon_class.indexOf('admin') >= 0 ) return;

        if (!this.permission.users) {
            this.permission.users = [];
        }
        this.permission.users.push(user);
        this.usersGridOption.setGridData(this.permission.users);
        this._filterAvailableUsers();
        /*
         if (user.name) {
         this.users = _.remove(this.permission.users, {name: user.name});
         this.usersGrid.push({principal: user.name});
         }
         else {
         this.users = _.remove(this.permission.users, user);
         this.usersGrid.push(user);
         }
         this.usersGridOption.setGridData(this.usersGrid);
         */
    }

    setAnyRepoOfType(type, isAnyRepoOfThisTypeCheckboxIsChecked) {
        if (isAnyRepoOfThisTypeCheckboxIsChecked) {
            this.permission.availableRepoKeys.forEach((repo) => {
                if (repo.type == type) {
                    repo["__fixed__"] = true;
                    if (!_.contains(this.permission.repoKeys, repo)) {
                        this.permission.repoKeys.push(repo);
                    }
                }
            });
            this.permission.repoKeys.forEach((repo) => {
                if (repo.type == type) {
                    repo["__fixed__"] = true;
                }
            });
            _.remove(this.permission.availableRepoKeys, {type: type});
        }
        else {
            this.permission.repoKeys.forEach((repo) => {
                if (repo.type == type) {
                    if (!_.contains(this.permission.availableRepoKeys, repo)) {
                        this.permission.availableRepoKeys.push(repo);
                    }
                }
            });
            _.remove(this.permission.repoKeys, {type: type});
        }
        this.dndComm.updateFilter();
    }

    _createGroupsGrid() {
        this.groupsGridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setRowTemplate('default')
                .setMultiSelect()
                .setColumns(this._getGroupsColumns())
                .setButtons(this._getGroupsActions())
                .setGridData([])
                .setBatchActions(this._getGroupsBatchActions());
    }

    _createUsersGrid() {
        this.usersGridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setRowTemplate('default')
                .setMultiSelect()
                .setColumns(this._getUsersColumns())
                .setGridData([])
                .setButtons(this._getUsersActions())
                .setBatchActions(this._getUsersBatchActions());

        this.usersGridOption.isRowSelectable = (row) => {
            return row.entity.principal !== this.user.name;
        }
    }

    _getUsersColumns() {
        return [
            {
                name: 'User',
                displayName: 'User',
                field: 'principal',
                cellTemplate: `<div class="principal-cell">
                                    <i ng-class="row.entity.icon_class ? row.entity.icon_class : 'icon-blank'"
                                       jf-tooltip="{{ ( row.entity.icon_class && row.entity.icon_class.indexOf('admin') >= 0 ) ? 'Admin Privlages' : row.entity.icon_tooltip}}"
                                       class="icon pull-left"></i>
                                       <span>{{row.entity.principal}}</span>
                               </div>`,
                width: '26%'
            },
            {
                name: 'Manage',
                displayName: 'Manage',
                field: 'managed',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.managed',
                        'row.entity.managed?row.entity.delete=row.entity.deploy=row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '12%'
            },
            {
                name: 'Delete/Overwrite',
                displayName: 'Delete/Overwrite',
                field: 'delete',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.delete',
                        'row.entity.delete?row.entity.deploy=row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '20%'
            },
            {
                name: 'Deploy/Cache',
                displayName: 'Deploy/Cache',
                field: 'deploy',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.deploy',
                        'row.entity.deploy?row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '20%'
            },
            {
                name: 'Annotate',
                displayName: 'Annotate',
                field: 'annotate',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.annotate',
                        'row.entity.annotate?row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '12%'
            },
            {
                name: 'Read',
                displayName: 'Read',
                field: 'read',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.read',null,
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '10%',
                minWidth: '100'
            }
        ]
    }

    _getGroupsColumns() {
        return [
            {
                name: 'Group',
                displayName: 'Group',
                field: 'principal',
                cellTemplate: `<div class="principal-cell">
                                    <i ng-class="row.entity.icon_class ? row.entity.icon_class : 'icon-blank'"
                                       jf-tooltip="{{( row.entity.icon_class && row.entity.icon_class.indexOf('admin') >= 0 ) ? 'Admin Privlages' : row.entity.icon_tooltip}}"
                                       class="icon pull-left"></i>
                                       <span>{{row.entity.principal}}</span>
                               </div>`,
                width: '26%'
            },
            {
                name: 'Manage',
                displayName: 'Manage',
                field: 'managed',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.managed',
                        'row.entity.managed?row.entity.delete=row.entity.deploy=row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '12%'
            },
            {
                name: 'Delete/Overwrite',
                displayName: 'Delete/Overwrite',
                field: 'delete',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.delete',
                        'row.entity.delete?row.entity.deploy=row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '20%'
            },
            {
                name: 'Deploy/Cache',
                displayName: 'Deploy/Cache',
                field: 'deploy',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.deploy',
                        'row.entity.deploy?row.entity.annotate=row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '20%'
            },
            {
                name: 'Annotate',
                displayName: 'Annotate',
                field: 'annotate',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.annotate',
                        'row.entity.annotate?row.entity.read=true:null',
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '12%'
            },
            {
                name: 'Read',
                displayName: 'Read',
                field: 'read',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.read',null,
                        'grid.appScope.PermissionForm.isDisable(row.entity)'),
                width: '10%',
                minWidth: '100'
            }
        ]
    }

    _getUsersBatchActions() {

        return [
            {
                icon: 'clear',
                name: 'Remove',
                callback: () => this._deleteSelectedUsers()
            }
        ]
    }

    _getGroupsBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Remove',
                callback: () => this._deleteSelectedGroups()
            }
        ]
    }

    _deleteSelectedGroups() {
        let self = this;
        let selectedGroups = this.groupsGridOption.api.selection.getSelectedRows();
        let confirmMessage = 'Are you sure you wish to delete ' + selectedGroups.length;

        selectedGroups.forEach((group) => {
            _.remove(this.permission.groups, group);
        });
        this.groupsGridOption.setGridData(this.permission.groups);
        this._filterAvailableGroups();
    }

    _deleteSelectedUsers() {
        let selectedUsers = this.usersGridOption.api.selection.getSelectedRows();
        selectedUsers.forEach((user) => {
            _.remove(this.permission.users, user);
        });
        this.usersGridOption.setGridData(this.permission.users);
        this._filterAvailableUsers();
    }

    _getGroupsActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Remove',
                callback: row => this._deleteGroup(row)
            }
        ]
    }

    _deleteGroup(row) {
        _.remove(this.permission.groups, row);
        this.groupsGridOption.setGridData(this.permission.groups);
        this._filterAvailableGroups();

    }

    _getUsersActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Remove',
                callback: row => this._deleteUser(row),
                // visibleWhen: row => row.principal !== this.user.name
            }
        ]
    }

    _deleteUser(row) {
        //    this.modal.confirm('Are you sure you wish to delete this user?')
        //      .then(()=> {
        _.remove(this.permission.users, row);
        this.usersGridOption.setGridData(this.permission.users);

        this._filterAvailableUsers();

        /*
         _.remove(this.usersGrid, row);
         this.permission.users.push(row);
         this.usersGridOption.setGridData(this.usersGrid);
         */
        //    });
    }

    save() {
        if (this.savePending) {
            return;
        }

        this.savePending = true;

        this.permission.includePattern = this.permission.includePatternArray.join(',') || '';
        this.permission.excludePattern = this.permission.excludePatternArray.join(',') || '';

        if (this.newPermission) {
            this.permissionsDao.create(this.permission).$promise.then(() => {
                this.savePending = false;
                this.ArtifactoryModelSaver.save();
                this.$state.go('^.permissions')
            }).catch(() => this.savePending = false);
        }
        else {
            this.permissionsDao.update(this.permission).$promise.then(() => {
                this.savePending = false;
                this.ArtifactoryModelSaver.save();
                this.$state.go('^.permissions')
            }).catch(() => this.savePending = false);
        }
    }

    isDisableRepositories() {
        return !this.user.isAdmin() && !this.newPermission;
    }

    isDisable(row) {
        return row.icon_class === 'icon-admin-new';
    }

    isDisableManager(row) {
        return row.principal === this.user.name;
    }

    _filterAvailableGroups() {
        let availableGroups = _.filter(this.allGroupsData, (group) => {
            return _.findWhere(this.permission.groups, {principal: group.principal}) === undefined;
        });

        this.availableGroups = this.getEntitiyGroupPermissionsList('group',availableGroups);
    }

    _filterAvailableUsers() {
        let availableUsers = _.filter(this.allUsersData, (user) => {
            return _.findWhere(this.permission.users, {principal: user.principal}) === undefined;
        });

        this.availableUsers = this.getEntitiyGroupPermissionsList('user',availableUsers);
    }

    formatNewPattern(newPattern){
        return _.trimLeft(newPattern,'/');
    }


    /**
    *   Build a map of an entity group as following: entityName => isAdmin
     */
    getEntityGroupMap(allEntitysData){
        let entityAdminMap = allEntitysData.map((entity)=>{
            return [entity.principal,entity.admin];
        });
        return new Map(entityAdminMap);
    }

    /**
     *  Get an entity group permissions array
     * */
    getEntitiyGroupPermissionsList(entityType,entities,isAdminMap){
        return _.map(entities, (entity) => {
            return this.getEntityPermissionObject(entityType,entity,isAdminMap);
        });
    }

    /**
    *  Get an entity's permission object
    * */
    getEntityPermissionObject(entityType,entity,isAdminMap){
        let isAdmin = (isAdminMap && isAdminMap.get(entity.principal)) || entity.admin;
        return {
            name: (entity.principal || entity.name || ''),
            principal: (entity.principal || entity.name || ''),
            icon_class: isAdmin ? 'icon-admin-new' : 'icon-blank',
            icon_tooltip: isAdmin ?  this.TOOLTIP.adminIcon[entityType] : '',
            annotate: entity.annotate || false,
            delete: entity.delete || false,
            deploy: entity.deploy || false,
            managed: entity.managed || false,
            read: entity.read || false,
            clickable: !isAdmin,
            mask: entity.mask || 31
        };
    }
}
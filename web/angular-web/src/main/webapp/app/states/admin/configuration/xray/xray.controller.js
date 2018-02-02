import EVENTS from "../../../../constants/artifacts_events.constants";
import FIELD_OPTIONS from "../../../../constants/field_options.constats";
import CONFIG_MESSAGES from "../../../../constants/configuration_messages.constants";

export class AdminConfigurationXrayController {

    constructor($scope, XrayDao, FooterDao, JFrogModal, JFrogGridFactory, JFrogEventBus, commonGridColumns) {
        this.$scope = $scope;
        this.commonGridColumns = commonGridColumns;
        this.xrayDao = XrayDao;
        this.JFrogEventBus = JFrogEventBus;
        this.footerDao = FooterDao;
        this.artifactoryGridFactory = JFrogGridFactory;
        this.modal = JFrogModal;
        this.notConnnectedToXrayMessage = CONFIG_MESSAGES.admin.configuration.xray.notConnnectedToXrayMessage;
        this.createGrid();
        this.getData();

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.REFRESH_PAGE_CONTENT,()=>{
            this.getData();
        });
//        XrayDao.getConf().$promise.then((data)=>console.log(data))
    }
    getData() {
        this.xrayDao.getIndex().$promise.then((data)=>{
            _.forEach(data, (row) => {

                let rowPackageType =_.find(FIELD_OPTIONS.repoPackageTypes, (type) => {
                    return type.value == row.pkgType.toLowerCase();
                });

                row.packageIcon = rowPackageType.icon;
                row.repoType = row.type.charAt(0).toUpperCase() + row.type.slice(1);
            });
            this.indexedRepos = data;
            this.gridOptions.setGridData(data);
        });
        this.xrayDao.getNoneIndex().$promise.then((data)=>{

            _.forEach(data, (row) => {

                let rowPackageType =_.find(FIELD_OPTIONS.repoPackageTypes, (type) => {
                    return type.value == row.pkgType.toLowerCase();
                });

                row.packageIcon = rowPackageType.icon;
                row.repoType = row.type.charAt(0).toUpperCase() + row.type.slice(1);
            });

            this.unindexedRepos = data;
        });
        this.xrayDao.isXrayEnabled().$promise.then((data)=>{
            this.xrayEnabled = data.xrayEnabled;
        });
    }

    createGrid() {
        this.gridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this.getColumns())
                .setMultiSelect()
                .setButtons(this.getActions())
                .setRowTemplate('default')
                .setBatchActions(this.getBatchActions());
    }

    getColumns() {
        return [
            {
                name: 'Name',
                displayName: 'Name',
                field: "name",
                width: '40%'
            },
            {
                name: 'Repository Type',
                displayName: 'Repository Type',
                field: "type",
                cellTemplate: '<div class="ui-grid-cell-contents">{{row.entity.repoType}}</div>',
                width: '30%'
            },
            {
                name: 'Package Type',
                displayName: 'Package Type',
                field: "pkgType",
                cellTemplate: this.commonGridColumns.iconColumn('row.entity.pkgType', 'row.entity.packageIcon', 'repo-type-icon'),
                width: '30%'
            }
        ]
    }

    getActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Remove',
                callback: (row) => this.removeIndexes(row),
                visibleWhen: () => this.xrayEnabled
            }
        ];
    }
    getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Remove',
                callback: () => this.bulkRemove(),
                visibleWhen: () => this.xrayEnabled
            }
        ]
    }

    add() {
        if (this.xrayEnabled === true) {
            let modalScope = this.$scope.$new();
            let modalInstance;

            modalScope.availableRepos = _.cloneDeep(this.unindexedRepos);
            modalScope.indexedRepos = _.cloneDeep(this.indexedRepos) || [];

            modalScope.save = (indexed) => {
                this.xrayDao.updateRepositories({}, indexed).$promise.then(() => {
                    this.getData();
                    modalInstance.close();
                });

            };

            modalInstance = this.modal.launchModal('add_xray_index_modal', modalScope);
        }
    }

    removeIndexes(repos) {
        if (_.isArray(repos)) {
            this.xrayDao.removeIndex({},repos).$promise.then(() => {
                this.getData();
            });
        }
        else {
            this.xrayDao.removeIndex({},[{name: repos.name,type: repos.type}]).$promise.then(() => {
                this.getData();
            });
        }
    }

    bulkRemove() {
        let selectedRows = this.gridOptions.api.selection.getSelectedRows();
        let removeList = _.map(selectedRows,(obj) => _.pick(obj,['name','type']));
        this.removeIndexes(removeList);
    }

    _updateFooter() {
        this.JFrogEventBus.dispatch(EVENTS.FOOTER_REFRESH);
    }

    xrayEnabledChange() {
        if (this.xrayEnabled === false) {
            this.modal.confirm("If disabled, Artifactory will not generate events for Xray and your artifacts will not be indexed or scanned.<br /><br />Are you sure you want to disable Xray integration?", 'Disable Xray Integration', {confirm: 'Disable'})
                .then(()=> {
                    this.enableDisableXrayIntegration(false);
                }).catch(() => {
                    this.xrayEnabled = true;
            });
        } else {
            this.enableDisableXrayIntegration(true);
        }
    }

    enableDisableXrayIntegration(action) {
        this.xrayEnabled = action;
        this.xrayDao.setXrayEnabled({}, {xrayEnabled: this.xrayEnabled}).$promise.then(()=> {});
    }
}
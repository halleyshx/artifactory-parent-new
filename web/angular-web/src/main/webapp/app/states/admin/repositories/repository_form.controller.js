import fieldsValuesDictionary from "../../../constants/field_options.constats";
import TOOLTIP from "../../../constants/artifact_tooltip.constant";
import CONFIG_MESSAGES from "../../../constants/configuration_messages.constants";

export class AdminRepositoryFormController {
    constructor($q, $scope, $stateParams, $state, $timeout, $location, RepositoriesDao, PropertySetsDao, JFrogGridFactory,
            ReverseProxiesDao, JFrogModal, FooterDao, ArtifactoryFeatures, JFrogNotifications, commonGridColumns,
            ArtifactoryModelSaver, GeneralConfigDao, DockerStatusDao, GlobalReplicationsConfigDao, XrayDao, GoogleAnalytics) {
        this.$scope = $scope;
        this.$q = $q;
        this.currentTab = 'basic';
        this.$timeout = $timeout;
        this.$stateParams = $stateParams;
        this.$location = $location;
        this.propertySetsDao = PropertySetsDao;
        this.globalReplicationsConfigDao = GlobalReplicationsConfigDao;
        this.artifactoryGridFactory = JFrogGridFactory;
        this.commonGridColumns = commonGridColumns;
        this.notifications = JFrogNotifications;
        this.modal = JFrogModal;
        this.generalConfigDao = GeneralConfigDao;
        this.NO_VALUE_STRING = '** NO VALUE **';
        this.$state = $state;
        this.footerDao = FooterDao;
        this.GoogleAnalytics = GoogleAnalytics;
        this.repositoriesDao = RepositoriesDao;
        this.reverseProxiesDao = ReverseProxiesDao;
        this.xrayDao = XrayDao;
        this.newRepository = false;
        this.features = ArtifactoryFeatures;
        this.replicationsGridOption = {};
        this.replicationScope = $scope.$new();
        this.TOOLTIP = TOOLTIP.admin.repositories;
        this.optionalFormats = [
                                    {id: 'bz2', text:'Bzip2 (.bz2 extension)', selectedText: 'bz2', isSelected:true},
                                    {id: 'lzma', text:'LZMA (.lzma extension)', selectedText: 'lzma'},
                                    {id: 'xz', text:'XZ (.xz extension)', selectedText: 'xz'}
                                ];
        this.CONFIG_MESSAGES = CONFIG_MESSAGES.admin.repositories;
        this.DockerStatusDao = DockerStatusDao.getInstance();
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['repoInfo'],['replications.*.proxies']);
        this.bintrayAuthentication = true;

        this.reverseProxies = ["**"];

        this.reverseProxiesSelectizeConfig = {
            sortField: 'text',
            create: false,
            maxItems: 1
        };
        this._setupDistribution();
        this.licensesListConfig = {
            sortField: 'text',
            maxItems: null,
            plugins: ['remove_button']
        };

        this._createGrid();
        this.initRepoForm();
        this.repoType = this.$stateParams.repoType;
        if (this.$stateParams.repoKey) {
            this.title = "Edit " + this.$stateParams.repoKey + " Repository";
            this.newRepository = false;
            this.editRepository(this.$stateParams.repoKey);
        }
        else {
            this.newRepository = true;
            this.repoInfo = new RepositoriesDao();
            this.title = "New " + _.capitalize(this.repoType) + " Repository";
            this._initNewRepositoryTypeConfig();

            if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
                if (!this.repoInfo.basic) {
                    this.repoInfo.basic = {};
                    this.repoInfo.basic.contentSynchronisation = {};
                    this.repoInfo.basic.contentSynchronisation.statistics = {};
                    this.repoInfo.basic.contentSynchronisation.properties = {};
                }

                this.repoInfo.basic.contentSynchronisation.enabled = false;
                this.repoInfo.basic.contentSynchronisation.statistics.enabled = false;
                this.repoInfo.basic.contentSynchronisation.properties.enabled = false;
            }
        }
        this.packageType = fieldsValuesDictionary.repoPackageTypes;

        this._getBaseUrl();
        this._getGlobalReplicationsStatus();
        this.originalValueFlag = true;
        this.footerDao.get(true);

        this.repositoryFilterTooltip = this.TOOLTIP.rulesPopup.repositoryFilterTooltip;
        this.pathFilterToolip = this.TOOLTIP.rulesPopup.pathFilterToolip;
        this.noReplicationsMessage = this.CONFIG_MESSAGES.local.noReplicationsMessage;
    }

    isCurrentRepoType(type) {
        return this.repoType == type;
    }

    onFormatSelectionChange() {
        let formats = _.filter(this.optionalFormats, (format) => format.isSelected);
        this.repoInfo.typeSpecific.optionalIndexCompressionFormats = _.map(formats,'id');
    }

    _updateOptionalIndexCompressionFormats(optionalIndexCompressionFormats) {
        let result = _.map(this.optionalFormats, format => {
            return _.extend({}, format,{
                isSelected: optionalIndexCompressionFormats.indexOf(format.id) !== -1
            })
        });
        return result;
    }

    /**
     * init propertiesSets  and replication scope functions for modal and fields options
     */
    initRepoForm() {
        this.replicationScope.replication = {}; //to create a single replication
        this.replicationScope.testLocalReplicationUrl = (url)=>this.testLocalReplicationUrl(url);

        this.replicationScope.addReplication = (replication)=> this.addReplication(replication);
        this.replicationScope.closeModal = ()=>this.closeModal();
        this.replicationScope.RepositoryForm = this;
    }

    /**
     * run only if edit repository and get repository data
     */
    editRepository(repoKey) {
        this.repositoriesDao.getRepository({type: this.repoType, repoKey: repoKey}).$promise
                .then(info => {
                    this.repoInfo = info;
                    this.repoInfo.basic.includesPatternArray = this.repoInfo.basic.includesPattern ? this.repoInfo.basic.includesPattern.split(',') : [];
                    this.repoInfo.basic.excludesPatternArray = this.repoInfo.basic.excludesPattern ? this.repoInfo.basic.excludesPattern.split(',') : [];
                    if(this.repoInfo.typeSpecific.repoType === 'YUM'){
                        this.repoInfo.groupFileNamesArray = this.repoInfo.typeSpecific.groupFileNames ? this.repoInfo.typeSpecific.groupFileNames.split(',') : [];
                    }
                    let repoPackageType = _.where(fieldsValuesDictionary.repoPackageTypes, { 'serverEnumName': this.repoInfo.typeSpecific.repoType });

                    if (repoPackageType.length) {
                        this.repoInfo.typeSpecific.text = repoPackageType[0].text;
                        this.repoInfo.typeSpecific.icon = repoPackageType[0].icon;
                    }

                    if (this.repoInfo.typeSpecific.repoType === "Docker" && !this.features.isAol() && !this.features.isOss()) {
                        this._getReveresProxyConfigurations();
                    }

                    if (this.repoInfo.typeSpecific.repoType === "Debian" && this.repoInfo.typeSpecific.optionalIndexCompressionFormats !== undefined) {
                        this.optionalFormats = this._updateOptionalIndexCompressionFormats(this.repoInfo.typeSpecific.optionalIndexCompressionFormats);
                    }

                    if (this.repoInfo.replications && this.repoInfo.replications.length) {
                        this.repoInfo.cronExp = this.repoInfo.replications[0].cronExp;
                        this.repoInfo.enableEventReplication = this.repoInfo.replications[0].enableEventReplication;
                    }
                    if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
                        this._getRepositoriesByType();
                    }
                    if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE && this.repoInfo.replications) {
                        this.repoInfo.replication = this.repoInfo.replications[0];
                    }
                    else {
                        this.replicationsGridOption.setGridData(this.repoInfo.replications);
                    }
                    this._getFieldsOptions();

                    if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
                        if (this.repoInfo.advanced.network.installedCertificatesList) {
                            this.repoInfo.advanced.network.installedCertificatesList.unshift('');
                        }
                        this._detectSmartRepository(false).then(()=>{
                            this.lastSmartRemoteURL = this.repoInfo.basic.url;
                        });
                    }
                    if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE || this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL) {
                        if (this.repoInfo.typeSpecific.maxUniqueSnapshots === 0) {
                            this.repoInfo.typeSpecific.maxUniqueSnapshots = '';
                        }
                        if (this.repoInfo.typeSpecific.maxUniqueTags === 0) {
                            this.repoInfo.typeSpecific.maxUniqueTags = '';
                        }
                        if (this.repoInfo.advanced.cache && this.repoInfo.advanced.cache.keepUnusedArtifactsHours === 0) {
                            this.repoInfo.advanced.cache.keepUnusedArtifactsHours = '';
                        }
                    }
                    if (this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
                        if (!this.repoInfo.typeSpecific.authenticated && this.$location.search().code) {
                            this.openBintrayOAuthModal();
                        }

                        this.bintrayAuthentication = this.repoInfo.typeSpecific.premium ? true : false;
                        this.defaultNewRepoPrivateSwitch = this.repoInfo.basic.defaultNewRepoPrivate == true ? 'Private' : 'Public';
                        this.distributionType = this.repoInfo.basic.productName != null;
                        this.distributionRules = this.repoInfo.advanced.distributionRules;
                        if (this.features.isOss()) {
                            this.distributionRules = _.filter(this.distributionRules,(rule=> {
                                return _.contains(['Maven', 'Gradle', 'Ivy', 'SBT'],rule.type)
                            }))
                        }
                        this._createDistributionRulesGrid(); // Edit
                        this._setupLicenses();
                        this._setRulesPackages();
                    }
                    this.repoInfo.basic.selectedLocalRepositories = _.pluck(_.filter(this.repoInfo.basic.resolvedRepositories, (repo)=>{
                        return repo.type === 'local';
                    }),'repoName');
                    this.repoInfo.basic.selectedRemoteRepositories = _.pluck(_.filter(this.repoInfo.basic.resolvedRepositories, (repo)=>{
                        return repo.type === 'remote';
                    }),'repoName');
                    this.repoInfo.basic.selectedLocalRepositories.unshift('');

                    this.ArtifactoryModelSaver.save();
                });

    }

    _setDefaultProxy() {
        if (this.newRepository && this.fields.defaultProxy && _.has(this.repoInfo, 'advanced.network')) {
            !this.repoInfo.advanced.network.proxy ?
                    this.repoInfo.advanced.network.proxy = this.fields.defaultProxy : '';
        }
    }

    /**
     *test button  when adding new replication in local repository
     */
    testLocalReplicationUrl(url) {
        // Create a copy of the repo
        let testRepo = angular.copy(this.repoInfo);

        // Make sure replications is not null
        testRepo.replications = testRepo.replications || [];

        let testReplication;
        if (this.replicationScope.sourceReplication) {
            testReplication = _.findWhere(testRepo.replications, {url: this.replicationScope.sourceReplication.url});
            angular.copy(this.replicationScope.replication, testReplication);
        }
        else {
            testReplication = angular.copy(this.replicationScope.replication);
            testRepo.replications.push(testReplication);
        }

        testReplication.cronExp = this.repoInfo.cronExp;
        testReplication.nextTime = this.repoInfo.nextTime;
        testReplication.type = this.repoType;
        testReplication.enableEventReplication = this.repoInfo.enableEventReplication;

        this.repositoriesDao.testLocalReplication({replicationUrl: url}, testRepo);
    }

    testRemoteUrl() {
        this.repositoriesDao.testRemoteUrl(this.repoInfo).$promise.then((result)=> {
            //console.log(result);
        });

        this._detectSmartRepository();
    }

    _detectSmartRepository(showModal=true) {
        if (this.features.isOss()) {
            return this.$q.when();
        }

        let defer = this.$q.defer();
        this.smartRepoUnknownCapabilities = false;

        let repoInfoCopy = angular.copy(this.repoInfo);
        if (!repoInfoCopy.typeSpecific.repoType) {
            repoInfoCopy.typeSpecific.repoType = "Generic";
        }

        this.repositoriesDao.detectSmartRepository(repoInfoCopy).$promise.then((result)=> {
            if (result.artifactory && result.version && result.features.length) {
                if (!this.repoInfo.basic.contentSynchronisation.enabled || this.repoInfo.basic.url != this.lastSmartRemoteURL) {
                    this.repoInfo.basic.contentSynchronisation.enabled = true;
                    this.lastSmartRemoteURL = this.repoInfo.basic.url;
                    this.smartRepoFeatures = result.features;

                    if (localStorage.disableSmartRepoPopup !== "true" && showModal) {
                        let modalInstance;
                        let modalScope = this.$scope.$new();
                        modalScope.smartRepo = this.repoInfo.basic.contentSynchronisation;
                        modalScope.smartRepo.typeSpecific = this.repoInfo.typeSpecific;
                        modalScope.closeModal = () => modalInstance.close();
                        modalScope.options = {dontShowAgain: false};
                        modalScope.isSmartRepoSupportFeature = (featureName) => this.isSmartRepoSupportFeature(featureName);
                        modalScope.onDontShowAgain = () => {
                            localStorage.disableSmartRepoPopup = modalScope.options.dontShowAgain;
                        };
                        modalInstance = this.modal.launchModal('smart_remote_repository', modalScope);
                    }

                    defer.resolve(true);
                }
                else
                    defer.resolve(false);
            }
            else {
                if (result.artifactory && result.version === null) {
                    this.smartRepoUnknownCapabilities = true;
                }
                this.repoInfo.basic.contentSynchronisation.enabled = false;
                defer.resolve(false);
            }
        });

        return defer.promise;
    }

    isSmartRepoSupportFeature(featureName) {
        return _.findWhere(this.smartRepoFeatures,{name: featureName}) !== undefined;
    }

    onBlurCredentials() {
        if (this.smartRepoUnknownCapabilities && this.repoInfo.advanced.network.username && this.repoInfo.advanced.network.password) {
            this._detectSmartRepository();
        }
    }

    testRemoteReplication() {
        this.addReplication(this.repoInfo.replication);
        this.repositoriesDao.testRemoteReplication(this.repoInfo).$promise.then((result)=> {
            //            console.log(result);
        });
    }

    setSnapshotVersionBehavior() {
        if (this.repoInfo && this.repoInfo.typeSpecific && this.repoInfo.typeSpecific.snapshotVersionBehavior) {
            if (this.repoInfo.typeSpecific.snapshotVersionBehavior == 'NONUNIQUE') {
                //this.repoInfo.typeSpecific.maxUniqueSnapshots = 0;
                this.disableMaxUniqueSnapshots = true;
            }
            else {
                this.disableMaxUniqueSnapshots = false;
            }
        }
    }

    _initNewRepositoryTypeConfig() {
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL) {
            this.repoInfo.type = 'localRepoConfig';
        }
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            this.repoInfo.type = 'remoteRepoConfig';
        }
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
            this.repoInfo.type = 'virtualRepoConfig';
        }
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            this.repoInfo.type = 'distributionRepoConfig';
        }

        this._getDefaultModels()
            .then(()=> {
                this._getFieldsOptions()
                    .then(()=> {
                        this._setDefaultFields();
                        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
                            this.openBintrayOAuthModal();
                        } else {
                            this.openRepoTypeModal();
                        }
                        this.ArtifactoryModelSaver.save();
                    });

                if (this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL) {
                    this.repoInfo.type = 'localRepoConfig';
                }
                if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
                    this.repoInfo.type = 'remoteRepoConfig';
                }
                if (this.repoType == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
                    this.repoInfo.type = 'virtualRepoConfig';
                }

            });

    }

    /**
     * get all properties
     */
    _populateProperties() {
        return this.propertySetsDao.query({isRepoForm: true}).$promise.then((properites)=> {
            this.propertiesList = properites;
        });
    }

    /**
     * set dropdown options and default fields
     */
    _getFieldsOptions() {
        return this.repositoriesDao.getAvailableChoicesOptions().$promise.then((fields)=> {
            this.fields = fields;
            this.localChecksumPolicies = fieldsValuesDictionary['localChecksumPolicy'];
            this.localChecksumPoliciesKeys = Object.keys(this.localChecksumPolicies);
            this.remoteChecksumPolicies = fieldsValuesDictionary['remoteChecksumPolicy'];
            this.remoteChecksumPoliciesKeys = Object.keys(fieldsValuesDictionary['remoteChecksumPolicy']);
            fields.proxies = fields.proxies || [];
            fields.proxies.unshift('');
            fields.webStartKeyPairs = fields.webStartKeyPairs || [];
            fields.webStartKeyPairs.unshift('');
            this.repositoryLayouts = _.sortBy(fields.repositoryLayouts,(layout) => layout);
            this.xraySeverities = fields.xraySeverities;
            this.xraySeverities.unshift('');

            if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
                this.repositoryLayouts.unshift('');
            }
            if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
                this.ruleTokensByType = fields.distributionTokensByType;
                this.ruleTokensByLayout = fields.distributionTokensByLayout;
                this.ruleTokensByLayoutKeys = Object.keys(this.ruleTokensByLayout);

                this.distributionDefaultRules = fields.distributionDefaultRules;
                if (this.features.isOss()) {
                    this.distributionDefaultRules = _.filter(this.distributionDefaultRules,(rule=> {
                        return _.contains(['Maven', 'Gradle', 'Ivy', 'SBT'],rule.type)
                    }))
                }

                this.distributionDefaultProductRules = fields.distributionDefaultProductRules;

                if (this.newRepository) {
                    this.repoInfo.type = 'distributionRepoConfig';
                    this.distributionRules = this.distributionDefaultRules;
                    this._createDistributionRulesGrid();    // NEW
                }

            }

            this.remoteLayoutMapping = angular.copy(fields.repositoryLayouts);
            this.remoteLayoutMapping.unshift('');
            this.mavenSnapshotRepositoryBehaviors = fieldsValuesDictionary['snapshotRepositoryBehavior'];
            this.mavenSnapshotRepositoryBehaviorsKeys = Object.keys(fieldsValuesDictionary['snapshotRepositoryBehavior']);
            this.pomCleanupPolicies = fieldsValuesDictionary['pomCleanupPolicy'];
            this.pomCleanupPoliciesKeys = Object.keys(fieldsValuesDictionary['pomCleanupPolicy']);
            this.vcsGitProviderOptions = fieldsValuesDictionary['vcsGitProvider'];
            this.vcsGitProviderOptionsKeys = Object.keys(fieldsValuesDictionary['vcsGitProvider']);
            this.setSnapshotVersionBehavior();
            return this._populateProperties();
        });
    }

    /**
     * fetching from server the default data
     */
    _getDefaultModels() {
        return this.repositoriesDao.getDefaultValues().$promise.then((models)=> {
            this.defaultModels = models.defaultModels;
        });

    }


    /**
     * check and set current tab
     */
    setCurrentTab(tab) {
        if (this.features.isDisabled(tab)) {
            return;
        }
        this.currentTab = tab;
    }

    isCurrentTab(tab) {
        return this.currentTab === tab;
    }

    /**
     * handle save or update click
     */
    save() {

        let pending = this.repositoriesForm.repoKey.$pending;
        if (pending && pending.repoKeyValidator) {
            this.$timeout(()=>{
                this.save();
            },100)
            return;
        }

        if (this.savePending) return;
        if (!this.repositoriesForm.$valid) return;

        this.savePending = true;

        this.repoInfo.basic.includesPattern = this.repoInfo.basic.includesPatternArray.join(',') || undefined;
        this.repoInfo.basic.excludesPattern = this.repoInfo.basic.excludesPatternArray.join(',') || undefined;
        if(this.repoInfo.typeSpecific.repoType === 'YUM'){
            this.repoInfo.typeSpecific.groupFileNames =
                    this.repoInfo.groupFileNamesArray ? this.repoInfo.groupFileNamesArray.join(',') : undefined;
        }

        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL) {
            if (this.repoInfo.replications && this.repoInfo.replications.length) {
                this.saveCronAndEventFlagToAllReplicationsAndValidateHa();
            }
            //Warn user if saving cron expression without any replication config
            if (this.repoInfo.cronExp && (!this.repoInfo.replications || !this.repoInfo.replications.length)) {
                this.notifications.create({warn: 'A cron expression was entered without any replication configuration.'
                + '\nThe expression will not be saved.'
                });
            }
        }

        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            this._detectSmartRepository().then((result) => {
                if (!result) {

                }
            });

            //Add replication if exists:
            if (this.repoInfo.replication) {
                if (this.repoInfo.cronExp && this.repoInfo.replication.enabled
                    && (!this.repoInfo.advanced.network.username || !this.repoInfo.advanced.network.password)) {
                    this.notifications.create({
                        error: 'Pull replication requires non-anonymous authentication to the ' +
                        'remote repository.\nPlease make sure to fill the \'Username\' and \'Password\' fields in the '
                        + 'Advanced settings tab or remove the fields you filled in the replication tab.'
                    });
                    this.savePending = false;
                    return false;
                }
                this.addReplication(this.repoInfo.replication);
            }

            if (this.repoInfo.advanced.network.proxy === '') {
                delete this.repoInfo.advanced.network.proxy;
            }

            if (this.repoInfo.advanced.network.selectedInstalledCertificate == '') {
                delete this.repoInfo.advanced.network.selectedInstalledCertificate;
            }
        }
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            this.repoInfo.advanced.distributionRules = this.distributionRules;
        }
        this.$timeout(() => {
            this.save_update();
        }, 500);

    }

    /**
     * save or update wizard form
     */
    save_update(recursed = false,changeState = true) {
        let defer = this.$q.defer();

        if (!recursed && this.repoInfo.isType('cocoapods') && this.baseUrl === this.NO_VALUE_STRING) {
            this._showUrlBaseAlert().then((gotoGenConf)=>{
                if (gotoGenConf) {
                    this.save_update(true).then(()=>{
                        this.$state.go('admin.configuration.general',{focusOnBaseUrl: true});
                    });
                }
                else {
                    this.save_update(true);
                }
            }).catch(()=>this.savePending = false);
        }
        else {
            if (this.repoInfo && this.repoInfo.typeSpecific && this.repoInfo.typeSpecific.keyPair === '') {
                delete this.repoInfo.typeSpecific.keyPair;
            }
            if (this.newRepository) {
                this.repositoriesDao.save(this.repoInfo).$promise.then((result)=> {
                    this.ArtifactoryModelSaver.save();
                    this.savePending = false;

                    // Track new repository creation to Google Analytics
                    if (this.repoInfo && this.repoInfo.typeSpecific) {
                        if (_.contains(['local','remote','distribution'], this.repoType)) {
                            this.GoogleAnalytics.trackEvent('Admin' ,'Create Repo' , this.repoInfo.typeSpecific.repoType , null , this.repoType);
                        } else if (this.repoType === 'virtual') {
                            let virtualData = [];

                            // Enable Dependency Rewrite (bower/npm)
                            let repoType = this.repoInfo.typeSpecific.repoType;
                            if (repoType === 'Bower' || repoType === 'Npm') {
                                virtualData.push("enableDependencyRewrite:" + this.repoInfo.typeSpecific.enableExternalDependencies);
                            }

                            // Default Deployment Repository
                            let defaultDeploymentRepo = this.repoInfo.basic.defaultDeploymentRepo ? true : false;
                            virtualData.unshift('defaultDeploymentRepo:' + defaultDeploymentRepo);

                            this.GoogleAnalytics.trackEvent('Admin', 'Create Repo' , this.repoInfo.typeSpecific.repoType , this.repoInfo.basic.selectedRepositories.length , this.repoType , virtualData[0] || '', virtualData[1] || '');
                        }
                    }

                    if (this.repoInfo.typeSpecific.repoType === "Docker" && this.features.isAol()) {
                        let modalScope = this.$scope.$new();


                        let dockerData = {
                            repoKey : this.repoInfo.general.repoKey,
                            packageType: this.repoType
                        }

                        this.DockerStatusDao.get({repoKey: dockerData.repoKey}).$promise.then((data) => {
                            dockerData.hostname = data.hostname;
                            dockerData.dockerPath = data.hostname + '-' + dockerData.repoKey + '.jfrog.io';
                            dockerData.noDeployToLocal = !this.repoInfo.basic.defaultDeploymentRepo;
                            dockerData = this.getDockerSnippets(dockerData);
                            modalScope.dockerData = dockerData;

                            this.dockerPopup = this.modal.launchModal('new_docker_modal', modalScope).result;
                            this.dockerPopup.finally(()=>{
                                if (changeState) this.$state.go('^.list', {repoType: this.repoType});
                            });
                        });
                    }
                    else if (changeState) this.$state.go('^.list', {repoType: this.repoType});
                    defer.resolve();
                }).catch(()=>this.savePending = false);
            } else {
                this.repositoriesDao.update(this.repoInfo).$promise.then((result)=> {
                    this.ArtifactoryModelSaver.save();
                    this.savePending = false;

                    if (changeState) this.$state.go('^.list', {repoType: this.repoType});
                    defer.resolve();
                }).catch(()=>this.savePending = false);
            }
        }

        return defer.promise;
    }

    getDockerSnippets(dockerData){
        let allSnippets = {
            dockerLogin: {message: "According to the repository permission, you will need to login to your repository with docker login command", snippet: "docker login " + dockerData.dockerPath},
            dockerPull:  {message: "Pull an image.", snippet: "docker pull hello-world"},
            dockerTag:   {message: "Tag an image.", snippet: "docker tag hello-world " + dockerData.dockerPath + "/hello-world"},
            dockerPush:  {message: "Then push it to your repository.", snippet: "docker push " + dockerData.dockerPath + "/hello-world"},
            dockerPushIt: {message: "Then push it.", snippet: "docker push " + dockerData.dockerPath + "/hello-world"},
            dockerTest:  {message: "And to test deploy to virtual, tag an image.", snippet: "docker tag hello-world " + dockerData.dockerPath + "/hello-world"},
            dockerPullImageFromRepo: {message: "To pull an image from your repository.", snippet: "docker pull " + dockerData.dockerPath + "/hello-world"},
        }

        dockerData.snippets = [];
        if(this.repoInfo.typeSpecific.dockerApiVersion === 'V2'){
            dockerData.snippets.push(allSnippets.dockerLogin);
        }

        switch(dockerData.packageType) {
            case 'local':
                dockerData.snippets.push(allSnippets.dockerPull);
                dockerData.snippets.push(allSnippets.dockerTag);
                dockerData.snippets.push(allSnippets.dockerPush);
                break;
            case 'remote':
                dockerData.snippets.push(allSnippets.dockerPull);
                break;
            case 'virtual':
                if (!dockerData.noDeployToLocal){
                    dockerData.snippets.push(allSnippets.dockerPull);
                    dockerData.snippets.push(allSnippets.dockerTest);
                    dockerData.snippets.push(allSnippets.dockerPushIt);
                    dockerData.snippets.push(allSnippets.dockerPullImageFromRepo);
                } else {
                    dockerData.snippets.push(allSnippets.dockerPullImageFromRepo);
                }
                break;
        }
        return dockerData;
    }

    _showUrlBaseAlert() {
        let modalScope = this.$scope.$new();
        modalScope.context = 'cocoapods';
        return this.modal.launchModal('base_url_alert_modal', modalScope, 'md').result;
    }

    /**
     * button pre and  forward at the bottom page
     */
    prevStep() {
        if (this.currentTab == 'advanced') {
            this.setCurrentTab('basic');
        }
        else if (this.currentTab == 'replications') {
            this.setCurrentTab('advanced');
        }
        else if (this.currentTab == 'rules') {
            this.setCurrentTab('advanced');
        }
    }

    fwdStep() {
        if (this.currentTab == 'basic') {
            this.setCurrentTab('advanced');
            return;
        }
        if (this.currentTab == 'advanced' && this.repoType != fieldsValuesDictionary.REPO_TYPE.VIRTUAL && this.repoType != fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            this.setCurrentTab('replications');
        }
        if (this.currentTab == 'advanced' && this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            this.setCurrentTab('rules');
        }
    }

    /**
     * function for select package type
     */
    openRepoTypeModal() {
        let addTextBelowIcon = ['Bower', 'Chef', 'CocoaPods', 'Conan', 'Pypi', 'Puppet', 'Opkg', 'Composer', 'SBT', 'Gradle', 'Gems', 'NuGet', 'GitLfs','Generic','P2','VCS'];
        this.$repoTypeScope = this.$scope.$new();
        this.$repoTypeScope.packageTypes = this.getPackageType();
        this.$repoTypeScope.highlightCheck = (typeFilter,type) => {
            if (typeFilter) {
                let string = type.text.toLowerCase(),
                searchstring = typeFilter.toLowerCase().replace(/ /g,'');


                if (string.substr(0, searchstring.length) == searchstring) {
                    type.highlighted = true;
                    return true;
                } else {
                    type.highlighted = false;
                    return false;
                };
            }
        };
        this.$repoTypeScope.checkNoResults = (typeFilter) => {
            if (typeFilter && typeFilter.length > 0 && _.filter(this.packageType,(type)=>type.highlighted).length == 0) {
                return true;
            }
        };
        this.$repoTypeScope.isSelected = () => {
            let HighlightedListItems = _.filter(this.packageType,(type)=>type.highlighted);
            if (HighlightedListItems.length == 1) {
                return true;
            }
        };
        this.$repoTypeScope.selectPackage = () => {
            let selectedItem = _.filter(this.packageType,(type)=>type.highlighted);
            if (selectedItem.length == 1) {
                this.selectRepoType(selectedItem[0])
            }
        };

        _.map(this.$repoTypeScope.packageTypes,(type)=>{
            if (_.includes(addTextBelowIcon, type.serverEnumName)) type.helpText = true;
        });


        this.$repoTypeScope.closeModal = () =>  this.closeModalPackageType();
        this.$repoTypeScope.modalClose = ()=> this.modalClose();
        this.$repoTypeScope.selectRepoType = (type)=>this.selectRepoType(type);
        this.isTypeModalOpen = true;

        this.repoTypeModal = this.modal.launchModal('repository_type_modal', this.$repoTypeScope, 930);
        this.repoTypeModal.result.finally(() => {
            this.repositoriesForm.repoKey.$validate();
            this.isTypeModalOpen = false;
            if (this.repoInfo.isType('debian')) {
                this.onFormatSelectionChange();
            }

        });
    }

    openBintrayOAuthModal() {
        this.$bintrayAuthScope = this.$scope.$new();
        //Stuff for outgoing request
        this.$bintrayAuthScope.isBackFromBintray = this.$location.search().code;
        this.$bintrayAuthScope.bintrayBaseUrl = this.repoInfo.typeSpecific.bintrayBaseUrl;
        this.$bintrayAuthScope.redirectUrl = encodeURIComponent(this.$location.absUrl());

        if (this.$bintrayAuthScope.isBackFromBintray) {
            this.$bintrayAuthScope.redirectUrl = encodeURIComponent(this.$location.absUrl().split('?')[0]);
            //this.$bintrayAuthScope.redirectUrl = encodeURIComponent(this.$location.absUrl().substring(0, this.$location.absUrl().indexOf('?')));
        } else {
            this.$bintrayAuthScope.redirectUrl = encodeURIComponent(this.$location.absUrl());
        }

        this.$bintrayAuthScope.config = {bintraySecretString : ''};
        this.$bintrayAuthScope.saveBintrayAuthInModel = () => this.saveBintrayAuthInModel(this.$bintrayAuthScope.config.bintraySecretString);
        this.isBintrayModalOpen = true;

        this.bintrayAuthModal = this.modal.launchModal('bintray_oauth_modal', this.$bintrayAuthScope, 600);
        this.bintrayAuthModal.result.then(() => {
            this.isBintrayModalOpen = false;
            this.repositoriesForm.repoKey.$validate();
        });
        this.bintrayAuthModal.result.catch(() => {
            this.ArtifactoryModelSaver.save();
            this.$state.go('^.list', {repoType: this.repoType});
        });
    }
    goToBintray() {
        let url = this.$bintrayAuthScope.bintrayBaseUrl + '/login/oauth/authorize?scope=org:?:admin&redirect_uri=' + this.$bintrayAuthScope.redirectUrl + '&artifactory_originated=Oik=';
        window.open(url, "_self");
    }
    closeModalPackageType() {
        if (!this.repoType) {
            return false;
        }
        if (this.newRepository) {
            this.setRepoLayout();
        }
        if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
            // Resetting resolved and selected repositories lists in case we are changing package type
            if (this.newRepository) {
                this.repoInfo.basic.selectedRepositories = [];
                this.repoInfo.basic.resolvedRepositories = [];
            }
            this._getRepositoriesByType();
        }
    }

    _getRepositoriesByType() {
        this.repositoriesDao.availableRepositoriesByType({
            type: this.repoInfo.typeSpecific.repoType,
            repoKey: this.repoInfo.general ? this.repoInfo.general.repoKey : ''
        }).$promise.then((repos)=> {
                    repos.availableLocalRepos = _.map(repos.availableLocalRepos, (repo)=> {
                        return {
                            repoName: repo,
                            type: 'local',
                            _iconClass: "icon icon-local-repo"
                        }
                    });
                    repos.availableRemoteRepos = _.map(repos.availableRemoteRepos, (repo)=> {
                        return {
                            repoName: repo,
                            type: 'remote',
                            _iconClass: "icon icon-remote-repo"
                        };
                    });
                    repos.availableVirtualRepos = _.map(repos.availableVirtualRepos, (repo)=> {
                        return {
                            repoName: repo,
                            type: 'virtual',
                            _iconClass: "icon icon-virtual-repo"
                        };
                    });

                    this.repoInfo.basic.selectedRepositories = _.map(this.repoInfo.basic.selectedRepositories,
                            (repo)=> {
                                if (repo.type == 'local') {
                                    return {
                                        repoName: repo.repoName,
                                        type: 'local',
                                        _iconClass: "icon icon-local-repo"
                                    }
                                }
                                else if (repo.type == 'remote') {
                                    return {
                                        repoName: repo.repoName,
                                        type: 'remote',
                                        _iconClass: "icon icon-remote-repo"
                                    }
                                }
                                else if (repo.type == 'virtual') {
                                    return {
                                        repoName: repo.repoName,
                                        type: 'virtual',
                                        _iconClass: "icon icon-virtual-repo"
                                    }
                                }
                            });


                    this.repositoriesList = [];
                    this.repositoriesList = repos.availableLocalRepos.concat(repos.availableRemoteRepos).concat(repos.availableVirtualRepos);

                    if (!this.newRepository) this.ArtifactoryModelSaver.save();

                });
    }

    getReplicationActions() {
        return [
            {
                icon: 'icon icon-run',
                tooltip: 'Run Now',
                visibleWhen: row => !this.globalReplicationsStatus.blockPushReplications && row.enabled,
                callback: row => this.executeReplicationNow(row)
            },
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: row => this._deleteReplication(row)
            }
        ]
    }

    executeReplicationNow(row) {
        if (true) {
            this.repositoriesDao.executeReplicationNow({replicationUrl: row.url},
                    this.repoInfo).$promise.then((result)=> {
                        //console.log(result)
                    });
        }
    }

    setRepoLayout() {
        let foundLayout = false;
        if (_.has(this.repoInfo, 'typeSpecific.repoType')) {
            let type = this.repoInfo.typeSpecific.repoType.toLowerCase();
            let defaultLayouts = fieldsValuesDictionary['defaultLayouts'];
            if (!this.repoInfo.basic) {
                this.repoInfo.basic = {};
                this.repoInfo.basic.repositoryLayout = {};
            }
            this.repositoryLayouts = _.filter(this.repositoryLayouts, (layout)=>{return layout !== ''})
            if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
                this.repositoryLayouts.unshift('');
            }

            let defaultLayout = defaultLayouts[type];
            if (defaultLayout && _.includes(this.repositoryLayouts, defaultLayout)) {
                this.repoInfo.basic.layout = defaultLayout;
                foundLayout = true;
            } else {
                this.repositoryLayouts.forEach((layout)=> {
                    if (layout.indexOf(type) != -1) {
                        this.repoInfo.basic.layout = layout;
                        foundLayout = true;
                    }
                });
            }
            if (!foundLayout) {
                this.repoInfo.basic.layout = "simple-default";
            }
        }
    }

    /**
     * set default fields for new repository
     */
    _setDefaultValuesByType() {
        if (!(this.repoInfo && this.repoInfo.typeSpecific)) {
            this.repoInfo.typeSpecific = {};
        }
        let type = this.repoInfo.typeSpecific.repoType.toLowerCase();
        if (type && this.defaultModels[type]) {
            angular.extend(this.repoInfo.typeSpecific, this.defaultModels[type]);
            // add default remote url for remote repository
            if (this.repoType.toLocaleLowerCase() == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
                this.repoInfo.basic.url = this.defaultModels[type].url;
            }
        }
        if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.VIRTUAL) {
            this.repoInfo.basic.repositoryLayout = '';
        }
    }

    _setDefaultFields() {
        if (!this.repoInfo.typeSpecific) {
            this.repoInfo.typeSpecific = {};
        }
        this.repoInfo.advanced = {};
        this.repoInfo.advanced.cache = {};
        this.repoInfo.advanced.network = {};

        _.forEach(this.defaultModels, (item) => {
            if (item.maxUniqueTags === 0) {
                item.maxUniqueTags = '';
            }
            if (item.maxUniqueSnapshots === 0) {
                item.maxUniqueSnapshots = '';
            }
            if (item.keepUnusedArtifactsHours === 0) {
                item.keepUnusedArtifactsHours = '';
            }
        });


        angular.extend(this.repoInfo.advanced.cache, this.defaultModels['cache']);
        angular.extend(this.repoInfo.advanced.network, this.defaultModels['network']);
        if (this.repoInfo.advanced.network.installedCertificatesList) {
            this.repoInfo.advanced.network.installedCertificatesList.unshift('');
        }

        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            if (!this.repoInfo.advanced) {
                this.repoInfo.advanced = {};
            }
            if (!this.repoInfo.basic) {
                this.repoInfo.basic = {};
            }

            angular.extend(this.repoInfo.advanced, this.defaultModels['remoteAdvanced']);
            angular.extend(this.repoInfo.basic, this.defaultModels['remoteBasic']);
        }
        else if (this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL
                    || this.repoType == fieldsValuesDictionary.REPO_TYPE.VIRTUAL
                    || this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            if (!this.repoInfo.advanced) {
                this.repoInfo.advanced = {};
            }
            if (!this.repoInfo.basic) {
                this.repoInfo.basic = {};
            }
            angular.extend(this.repoInfo.advanced, this.defaultModels['localAdvanced']);
            angular.extend(this.repoInfo.basic, this.defaultModels['localBasic']);
            this.repoInfo.typeSpecific.localChecksumPolicy = this.defaultModels['maven'].localChecksumPolicy;
            if(this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
                this.repoInfo.typeSpecific = this.defaultModels['distribution'];
                this.repoInfo.basic.layout = "simple-default";
                this.distributionType = false;
            }
        }

        this.repoInfo.basic.includesPatternArray = ['**/*'];
        this.repoInfo.basic.excludesPatternArray = [];

        this._setDefaultProxy();
    }

    selectRepoType(type) {
        if (this.features.isDisabled(type.value)) {
            return;
        }

        this.repoTypeModal.close();
        if (!this.repoInfo.typeSpecific) {
            this.repoInfo.typeSpecific = {};
        }
        this.repoInfo.typeSpecific.repoType = type.serverEnumName;
        this.repoInfo.typeSpecific.icon = type.icon;
        this.repoInfo.typeSpecific.text = type.text;
        if (this.repoInfo.typeSpecific.repoType === "Docker" && !this.features.isAol() && !this.features.isOss()) {
            this._getReveresProxyConfigurations();
        }
        if(this.repoInfo.typeSpecific.repoType === "CocoaPods" && this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            this.repoInfo.advanced.network.socketTimeout = 45000
        }

        if (this.newRepository) {
            this._setDefaultValuesByType();
        }
        this.closeModalPackageType();
    }

    saveBintrayAuthInModel(bintraySecretString) {
        //todo -- should respond to feature disabled?
        if (!this.repoInfo.typeSpecific) {
            this.repoInfo.typeSpecific = {};
        }
        this.repoInfo.typeSpecific.repoType = 'Distribution';
        if (this.newRepository) {
            this._setDefaultValuesByType();
        }
        this.repoInfo.typeSpecific.bintrayAuthString = bintraySecretString;
        this.repoInfo.typeSpecific.redirectUrl = this.$location.absUrl().split('?')[0];
        this.repoInfo.typeSpecific.paramClientId = this.$location.search().client_id;
        this.repoInfo.typeSpecific.code = this.$location.search().code;
        this.repoInfo.typeSpecific.scope = this.$location.search().scope;
        this.repositoriesDao.saveBintrayOauthConfig(this.repoInfo.typeSpecific).$promise.then((result)=> {
            this.bintrayAuthModal.close();
            //Result from backend contains the key for the newly created OAuth app that this repo must reference.
            this.repoInfo.typeSpecific = result.data;
            let isPremium = this.repoInfo.typeSpecific.premium;
            if (!isPremium) {
                this.bintrayAuthentication = false;
            }
            this._setupLicenses();
            this._checkVisibility(isPremium)
            this._setRulesPackages();
        }).catch(() => {

        });
    }

    isRightColumnEmptyInLocalRepo(){
        return (!this.repoInfo.isType('maven', 'gradle', 'ivy', 'sbt', 'yum','cocoapods','debian','docker','nuget'));
    }

    isRightColumnEmptyInRemoteRepo(){
        return (!this.repoInfo.basic.contentSynchronisation.enabled &&
               !this.smartRepoUnknownCapabilities &&
               !this.repoInfo.isType('maven','gradle','ivy','sbt','generic',
                                               'vcs', 'bower', 'cocoapods', 'composer',
                                               'docker','nuget','debian','yum','p2'));
    }

    /**
     * newReplication; editReplication->
     * functions for replications modal (work only for local repos)
     */
    newReplication() {
        if (this.repoInfo.replications && this.repoInfo.replications.length && this.features.isDisabled('highAvailability') && !this.features.isDedicatedAol()) {
            this.notifications.create({warn: 'Multi-push replication will only work with an Enterprise license'});
            return true;
        }
        this.replicationScope.replication = {};
        this.replicationScope.title = 'New Replication';
        this.replicationScope.replication.socketTimeout = 15000;
        this.replicationScope.replication.syncProperties = true;
        this.replicationScope.sourceReplication = null;
        this.replicationScope.replication.enabled = true;
        this.replicationModal(false);
    }


    editReplication(row) {
        this.replicationScope.title = 'Replication Properties';
        this.replicationScope.replication = angular.copy(row);
        this.replicationScope.sourceReplication = row;
        this.replicationModal(true);
    }

    _deleteReplication(row) {
        this.modal.confirm("Are you sure you wish to delete this replication?", 'Delete Replication', {confirm: 'Delete'})
                .then(()=> {
                    _.remove(this.repoInfo.replications, row);
                    this.replicationsGridOption.setGridData(this.repoInfo.replications);
                });

    }

    replicationModal(isEdit) {
        this.replicationScope.replication.proxies = this.fields.proxies;
        if(!isEdit) {
            this.fields.defaultProxy ? this.replicationScope.replication.proxy = this.fields.defaultProxy : '';
        }
        this.modalInstance = this.modal.launchModal('replication_modal', this.replicationScope);
    }

    /**
     * add replication: function that save fields in form for replication.
     * if local: push it for grid replication
     * if remote: clear exsit replication and set the new one
     */
    addReplication(replication) {

        if (this.repoType.toLowerCase() == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            this.repoInfo.replications = [];
        }
        replication.enabled = replication.enabled ? replication.enabled : false;
        replication.syncDeletes = replication.syncDeletes ? replication.syncDeletes : false;
        replication.syncProperties = replication.syncProperties ? replication.syncProperties : false;
        replication.syncStatistics = replication.syncStatistics ? replication.syncStatistics : false;
        replication.cronExp = this.repoInfo.cronExp;
        replication.nextTime = this.repoInfo.nextTime;
        replication.enableEventReplication = this.repoInfo.enableEventReplication;
        replication.type = this.repoType;
        if (replication.proxy === '') {
            delete replication.proxy;
        }
        if (this.replicationScope.sourceReplication) {
            // updating replication
            angular.copy(replication, this.replicationScope.sourceReplication);
        } else {
            // adding new replication
            this.repoInfo.replications = this.repoInfo.replications || [];
            this.repoInfo.replications.push(replication);
        }
        if (this.repoType.toLocaleLowerCase() == fieldsValuesDictionary.REPO_TYPE.LOCAL) {
            this.replicationsGridOption.setGridData(this.repoInfo.replications);
            this.closeModal();
        }
    }

    /**
     * Saves the cron expression and event replication flag to all replications.
     * Also validates that if HA license is not installed - only one active replication is saved.
     */
    saveCronAndEventFlagToAllReplicationsAndValidateHa() {
        //Signifies save should disable all replications but one because multiple enabled replicaions exist without HA license
        let notHa = this.features.isDisabled('highAvailability') && !this.features.isDedicatedAol() && this.repoInfo.replications.length > 1;
        this.repoInfo.replications.forEach((replication) => {
            replication.cronExp = this.repoInfo.cronExp;
            replication.enableEventReplication = this.repoInfo.enableEventReplication;
            if(notHa) {
                replication.enabled = false;
            }
        });
        if(notHa) {
            this.notifications.create({warn: 'You saved multiple enabled replication configurations.\n Multi-push ' +
            'replication is only available with an Enterprise licenses therefore only the first replication will be' +
            'saved as enabled and the rest will be disabled.'});
            this.repoInfo.replications[0].enabled = true;
        }
    }

    closeModal() {
        this.modalInstance.close();
    }

    _createGrid() {
        this.replicationsGridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this._getColumns())
                .setSingleSelect()
                .setRowTemplate('default')
                .setButtons(this.getReplicationActions())
                .setGridData([]);
    }

    /**
     * controller display arrows form
     */
    showNextButton() {
        if (this.repoType == fieldsValuesDictionary.REPO_TYPE.LOCAL || this.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE) {
            if (this.features.isDisabled('replications')) {
                return this.currentTab != 'advanced';
            }
            return this.currentTab != 'replications';
        } else if (this.repoType == fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION) {
            if (this.features.isDisabled('rules')) {
                return this.currentTab != 'advanced';
            }
            return this.currentTab != 'rules';
        }
        else {
            return this.currentTab != 'advanced';
        }
    }

    _getColumns() {
        return [
            {
                name: 'URL',
                displayName: 'URL',
                field: 'url',
                cellTemplate: '<div class="ui-grid-cell-contents"><a ng-click="grid.appScope.RepositoryForm.editReplication(row.entity)">{{row.entity.url}}</a></div>'

            },
            {
                name: 'Sync Deletes',
                displayName: 'Sync Deletes',
                field: 'syncDeletes',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.syncDeletes')
            },
            {
                name: 'Sync Properties',
                displayName: 'Sync Properties',
                field: 'syncProperties',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.syncProperties')
            },
            {
                name: 'Enabled',
                displayName: 'Enabled',
                field: 'enabled',
                cellTemplate: this.commonGridColumns.checkboxColumn('row.entity.enabled')
            }
        ]
    }

    /**
     * all packages sorts by type
     */
    getPackageType() {
        switch (this.repoType) {
            case fieldsValuesDictionary.REPO_TYPE.LOCAL:
            {
                return _.filter(this.packageType,(type) => {
                   return _.indexOf(type.repoType, fieldsValuesDictionary.REPO_TYPE.LOCAL) != -1});
            }
            case fieldsValuesDictionary.REPO_TYPE.REMOTE:
            {
                return _.select(this.packageType,(type) => {
                    return _.indexOf(type.repoType, fieldsValuesDictionary.REPO_TYPE.REMOTE) != -1});
            }
            case fieldsValuesDictionary.REPO_TYPE.VIRTUAL:
            {
                return _.select(this.packageType,(type) => {
                    return _.indexOf(type.repoType, fieldsValuesDictionary.REPO_TYPE.VIRTUAL) != -1});
            }
            case fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION:
            {
                return fieldsValuesDictionary.REPO_TYPE.DISTRIBUTION
            }
        }
    }

    cancel() {
        this.$state.go('^.list', {repoType: this.repoType});
    }

    _getReveresProxyConfigurations() {
        this.reverseProxiesDao.get().$promise.then((reverseProxies)=> {

            this.reverseProxyConfigured = reverseProxies.serverName && reverseProxies.webServerType && (reverseProxies.useHttp || reverseProxies.useHttps) && reverseProxies.dockerReverseProxyMethod !== 'NOVALUE';;

//            this.hideReverseProxy = this.reverseProxyConfigured

            if (this.reverseProxyConfigured) {
                if (!this.repoInfo.advanced.reverseProxy) {
                    this.repoInfo.advanced.reverseProxy = {
                        key: reverseProxies.key,
                        serverName: reverseProxies.serverName
                    };
                }
                if (reverseProxies.dockerReverseProxyMethod === 'PORTPERREPO') {
                    this.reverseProxyPortMode = true;
                    this.repoInfo.advanced.reverseProxy.serverName = reverseProxies.serverName;
                }
                else {
                    this.reverseProxyPortMode = false;
                    if (this.repoInfo.general && this.repoInfo.general.repoKey) this.repoInfo.advanced.reverseProxy.serverName = reverseProxies.serverNameExpression.replace('*',this.repoInfo.general.repoKey);
                    this.reverseProxyServerNameExpression = reverseProxies.serverNameExpression;
                }
            }

        });
    }
    onChangeRepoKey() {
        if (this.repoInfo.general && this.repoInfo.general.repoKey && this.reverseProxyServerNameExpression) this.repoInfo.advanced.reverseProxy.serverName = this.reverseProxyServerNameExpression.replace('*',this.repoInfo.general.repoKey);
    }

    _getBaseUrl() {
        this.generalConfigDao.get().$promise.then((data) => {
            this.baseUrl = data.customUrlBase || this.NO_VALUE_STRING;
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

    // YUM FOLDER DEPTH TOOLTIP
    hostYumOriginalValue() {
        this.originalYumValue;
        if (this.originalValueFlag) {
            this.originalYumValue = this.repoInfo.typeSpecific.metadataFolderDepth;
            this.originalValueFlag = false;
        }
    }
    changeYumFolderDepth() {
        this.yumTooltip = (this.repoInfo.typeSpecific.metadataFolderDepth < this.originalYumValue) ? true : false;
    }

    // DISTRIBUTION RULES

    _setRulesPackages() {

        this.distributionRulesPackages = _.filter(this.packageType, (o) => {
            if (this.features.isOss()) {
                return o.value == 'generic' || o.value == 'maven' || o.value == 'gradle' || o.value == 'ivy' || o.value == 'sbt';
            }
            else {
                return o.value != 'gitlfs' && o.value != 'gems' && o.value != 'pypi' && o.value != 'p2' && o.value != 'vcs';
            }
        });
        this.distributionRulesPackages.forEach((pack)=>delete pack.description);
    }

    _checkVisibility(isPremium) {
        if (!isPremium) {
            this.defaultNewRepoPrivateSwitch = 'Public';
            this.repoInfo.basic.defaultNewRepoPrivate = false;
            this.repoInfo.basic.defaultNewRepoPremium = false;
        } else {
            this.defaultNewRepoPrivateSwitch = 'Private';
            this.repoInfo.basic.defaultNewRepoPrivate = true;
            this.repoInfo.basic.defaultNewRepoPremium = true;
        }
    }

    _setupLicenses() {
        // License input configuration
        this.licensesList = _.map(this.repoInfo.typeSpecific.availableLicenses, (lic) => {
            return {
                text: lic,
                value: lic
            }
        });
    }

    _setupDistribution() {
        this.rulesGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
            .setMultiSelect()
            .setColumns(this._getDistributionRulesColumns())
            .setButtons(this._getRulesActions())
            .setBatchActions(this._getBatchActions())
            .setDraggable(this._reorderRules.bind(this));


    }

    _reorderRules() {
        this.distributionRules = this.rulesGridOptions.data;
    }

    _getRulesActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: row => this._deleteRule(row)
            }
        ]
    }

    _deleteRule(row) {
        this.modal.confirm("Are you sure you wish to delete this rule?", 'Delete Rule', {confirm: 'Delete'})
            .then(()=> {
                _.remove(this.distributionRules, row);
                this._createDistributionRulesGrid();
            });

    }

    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this.bulkDelete()
            }
        ]
    }

    bulkDelete(){
        // Get All selected rules
        let selectedRows = this.rulesGridOptions.api.selection.getSelectedRows();

        // Ask for confirmation before delete and if confirmed then delete bulk of rules
        this.modal.confirm(`Are you sure you want to delete ${selectedRows.length} rules?`)
            .then(() => {
                this.distributionRules = _.filter(this.distributionRules, (row)=>{
                    return !_.find(selectedRows, {name: row.name})
                });
                this._createDistributionRulesGrid();
            });
    }

    changeDistribute() {
        if (this.distributionType == false) {
            this.repoInfo.basic.productName = null;
        }
        /*this.modal.confirm("Artifactory has a set of default rules for " + (this.distributionType ? "product" : "packages") + " distribution.<br/>Would you like to set these rules and <strong>override</strong> existing rules?", this.distributionType ? 'Set Product Distribution Rules' : 'Set Packages Distribution Rules', {confirm: 'Override'})
            .then(()=> {
                if (this.distributionType == false) {
                    this.distributionRules = this.distributionDefaultRules;
                }
                else {
                    this.distributionRules = this.distributionDefaultProductRules;
                }
                this._createDistributionRulesGrid();
            }).catch(() => {
            this.distributionType = !this.distributionType;
        });*/
    }

    changeDistributeVisibility() {
        this.repoInfo.basic.defaultNewRepoPrivate = this.defaultNewRepoPrivateSwitch == 'Private' ? true : false;
    }

    _createDistributionRulesGrid() {

        if (!this.distributionRules && !this.newRepository) {
            this.distributionRules = this.repoInfo.advanced.distributionRules;
        }

        _.forEach(this.distributionRules, (row) => {
            var rowPackageType =_.find(fieldsValuesDictionary.repoPackageTypes, (type) => {
                return type.value == row.type.toLowerCase();
            });
            if (rowPackageType) {
                row.displayType = rowPackageType.text;
                row.typeIcon = rowPackageType.icon;
            } else row.ignore = true;
        });
        let distRepoRulesGridData = _.filter(this.distributionRules, (row) => !row.ignore);
        this.rulesGridOptions.setGridData(distRepoRulesGridData);

    }

    _getDistributionRulesColumns() {
                return [
                    {
                        name: 'Name',
                        displayName: 'Name',
                        field: "name",
                        cellTemplate: '<div class="ui-grid-cell-contents"><a ng-click="grid.appScope.RepositoryForm.editDistributionRule(row.entity)" id="rule-name">{{row.entity.name}}</a></div>',
                        width: '85%',
                        enableSorting: false
                    },
                    {
                        name: 'Type',
                        displayName: 'Type',
                        field: 'displayType',
                        cellTemplate: this.commonGridColumns.iconColumn('row.entity.displayType', 'row.entity.typeIcon', 'repo-type-icon'),
                        width: '15%',
                        enableSorting: false
                    }
            ]

    }

    rulesPopup() {
        this.rulesModalScope = this.$scope.$new();
        this.rulesModalScope.title = "Add New Rule";
        this.rulesModalScope.itemToEdit = null;
        this.rulesModalScope.rule = {};
        this.availableTokens = null;
        this.rulesModalScope.repositoryFilterTooltip = this.repositoryFilterTooltip;
        this.rulesModalScope.pathFilterToolip = this.pathFilterToolip;

        this.modalRules = this.modal.launchModal('add_rule_modal', this.rulesModalScope, 1000);
    }

    changeRuleRepoType() {
        let selectedPackageType = this.rulesModalScope.rule.selectedPackageType;
        let selectedPackageServerEnumName = selectedPackageType.serverEnumName;
        let selectedPackageValue = selectedPackageType.value;
        if (selectedPackageValue == 'generic') {
            this.rulesModalScope.rule.RulePackageLayoutSelect = null; // Reset selected layout
            this.availableTokens = this.ruleTokensByLayout[selectedPackageServerEnumName];
        } else if (selectedPackageValue == 'nuget' || selectedPackageValue == 'debian') {
            this.rulesModalScope.rule.distributionCoordinatesPackage = "[packageName]";
            this.availableTokens = this.ruleTokensByType[selectedPackageServerEnumName];
        } else {
            this.availableTokens = this.ruleTokensByType[selectedPackageServerEnumName];
            if (!this.distributionType) {
                this.availableTokens = _.filter(this.availableTokens,(val) => val !== '${productName}');
            }
        }

        // Auto fill coordinates from default rules module
        let coordinates = {};
        if (selectedPackageValue != 'generic') {
            coordinates = _.filter(this.distributionDefaultRules, (o) => {
                return o.type.toLowerCase() === selectedPackageValue;
            });
            coordinates = coordinates.length ? coordinates[0].distributionCoordinates : [];
        }
        this.rulesModalScope.rule.distributionCoordinatesRepo = coordinates.repo || '';
        this.rulesModalScope.rule.distributionCoordinatesPackage = coordinates.pkg || '';
        this.rulesModalScope.rule.distributionCoordinatesVersion = coordinates.version || '';
        this.rulesModalScope.rule.distributionCoordinatesPath = coordinates.path || '';
    }

    changeRulePackageLayout() {
        this.availableTokens =  this.ruleTokensByLayout[this.rulesModalScope.rule.RulePackageLayoutSelect];
    }

    saveDistributionRule() {
        let ruleObject = {
                    name: this.rulesModalScope.rule.ruleName,
                    type: this.rulesModalScope.rule.selectedPackageType.text,
                    repoFilter: this.rulesModalScope.rule.filterRepo  || '',
                    pathFilter: this.rulesModalScope.rule.filterPath  || '',
                    distributionCoordinates: {
                        repo: this.rulesModalScope.rule.distributionCoordinatesRepo || '',
                        pkg: this.rulesModalScope.rule.distributionCoordinatesPackage || '',
                        version: this.rulesModalScope.rule.distributionCoordinatesVersion || '',
                        path: this.rulesModalScope.rule.distributionCoordinatesPath  || ''
                    }
        };

        if (this.rulesModalScope.itemToEdit == null) {
            this.distributionRules.push(ruleObject);
        } else {
            this.distributionRules[this.rulesModalScope.itemToEdit] = ruleObject;
        }

        this._createDistributionRulesGrid();
        this.modalRules.close();

    }

    editDistributionRule(row) {
        let selectedPackageType = _.find(fieldsValuesDictionary.repoPackageTypes, (type) => {
            return type.value == row.type.toLowerCase();
        });
        this.availableTokens = this.ruleTokensByType[row.type];

        if (!this.distributionType) {
            this.availableTokens = _.filter(this.availableTokens,(val) => val !== '${productName}');
        }

        this.rulesModalScope = this.$scope.$new();
        this.rulesModalScope.originalRuleName = row.name;
        this.rulesModalScope.title = "Edit Rule";
        this.rulesModalScope.itemToEdit = _.indexOf(this.distributionRules, row);
        this.rulesModalScope.rule = {
            ruleName: row.name,
            selectedPackageType: selectedPackageType,
            filterRepo: row.repoFilter,
            filterPath: row.pathFilter,
            distributionCoordinatesRepo: row.distributionCoordinates.repo,
            distributionCoordinatesPackage: row.distributionCoordinates.pkg,
            distributionCoordinatesVersion: row.distributionCoordinates.version,
            distributionCoordinatesPath: row.distributionCoordinates.path
        };

        this.rulesModalScope.repositoryFilterTooltip = this.repositoryFilterTooltip;
        this.rulesModalScope.pathFilterToolip = this.pathFilterToolip;

        this.modalRules = this.modal.launchModal('add_rule_modal', this.rulesModalScope, 1000);
    }

    // RULE TEST
    testRule() {
        this.repositoriesDao.testDistributionRules({
            testPath: this.rulesModalScope.rule.testPath,
            productName: this.repoInfo.basic.productName || null,
            name: this.rulesModalScope.rule.ruleName,
            type: this.rulesModalScope.rule.selectedPackageType.text,
            repoFilter: this.rulesModalScope.rule.filterRepo  || '',
            pathFilter: this.rulesModalScope.rule.filterPath  || '',
            distributionCoordinates: {
                repo: this.rulesModalScope.rule.distributionCoordinatesRepo || '',
                pkg: this.rulesModalScope.rule.distributionCoordinatesPackage || '',
                version: this.rulesModalScope.rule.distributionCoordinatesVersion || '',
                path: this.rulesModalScope.rule.distributionCoordinatesPath  || ''
            }
        }).$promise.then((result)=> {
            //console.log(result);
        });
    }

    // XRAY INTEGRATION
    changeXrayIndexCheckbox() {
        if (!this.repoInfo.basic.xrayConfig.enabled) {
            this.repoInfo.basic.xrayConfig.blockUnscannedArtifacts = false;
            this.repoInfo.basic.xrayConfig.minimumBlockedSeverity = '';
        }
    }

    changeXrayBlockSeverity() {
        if (!this.repoInfo.basic.xrayConfig.minimumBlockedSeverity
                || this.repoInfo.basic.xrayConfig.minimumBlockedSeverity === '') {
            this.repoInfo.basic.xrayConfig.blockUnscannedArtifacts = false;
        }
    }

    // VALIDATIONS
    isProductNameValid(value) {
            return !value || value.match(/^[a-zA-Z0-9\-_\.:]+$/)
    }
    checkUniqueRuleName(value) {
        let found = _.find(this.distributionRules, function(o) {
            return o.name == value;
        });
        return !found || value == this.rulesModalScope.originalRuleName;
    }
    checkReservedName(value) {
        let notAlowedStrings = ['delete', 'remove', 'edit', 'create', 'save', 'new', 'account', 'usage', 'anonymous', 'status', 'product', 'eula', 'packages', 'package', 'products', 'jcenter', 'rpm-center', 'deb-center', 'node-center', 'ruby-center', 'gems-center', 'gems-central', 'docker-'];
        let found = _.includes(notAlowedStrings, value);
        return !found;
    }

    validateRuleRepoName(value) {
        let regex = /^([A-Za-z0-9.\-_]+)$/;
        return !value || value.match(regex);
    }

    firstLetterValidation(value) {
        let firstLetterRegex = /^[A-Za-z0-9]/; //Allow letters and numbers in first letter
        return !value || value.charAt(0).match(firstLetterRegex);
    }

    checkLength(value,min=1,max=100) {
        if (value) {
            let result = (value.length > max) ? false : true;
            return (result);
        }
    }

    dockerVagrantValidate(value) {

        if (value) {
            if (this.rulesModalScope.rule.selectedPackageType && (this.rulesModalScope.rule.selectedPackageType.serverEnumName === 'Docker' || this.rulesModalScope.rule.selectedPackageType.serverEnumName === 'Vagrant')) {
                let regex = /^([a-z0-9.\-_]+)$/;
                return value.match(regex);
            } else {
                return true;
            }
        }
    }

    isEventReplicationDisabled() {
        return !this.repoInfo.replication ||
                !this.repoInfo.replication.enabled ||
                !this.lastSmartRemoteURL ||
                this.footerDao.getInfo().versionID !== 'ENT' ||
                !this.isSmartRepoSupportFeature('EVENT_BASED_PULL_REPLICATION');
    }

    formatNewPattern(newPattern) {
        return _.trimLeft(newPattern,'/');
    }
}
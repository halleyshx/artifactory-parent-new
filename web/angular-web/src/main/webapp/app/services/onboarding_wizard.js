import EVENTS from '../constants/artifacts_events.constants';
import fieldOptions from "../constants/field_options.constats";
import TOOLTIP from '../constants/artifact_tooltip.constant';

export class OnBoardingWizard {

    constructor($timeout, $q, JFrogModal, FooterDao, ArtifactoryState, GoogleAnalytics, ArtifactoryFeatures, OnboardingDao, User,$state) {
        this.ArtifactoryState = ArtifactoryState;
        this.GoogleAnalytics = GoogleAnalytics;
        this.FooterDao = FooterDao;
        this.$timeout = $timeout;
        this.features = ArtifactoryFeatures;
        this.$q = $q;
        this.modal = JFrogModal;
        this.User = User;
        this.OnboardingDao = OnboardingDao;
        this.$state = $state;
    }

    show(quickSetup = false) {


        //Pass thw quickSetup flag to the wizard controller
        WizardController.prototype.quickSetup = quickSetup;
        WizardController.prototype.mainCtrl = this;

        //We create a promise to be fulfilled when we've got both user data and footer (license/features) data
        let defer = this.$q.defer();
        this.User.loadUser(true).then(()=> {
            // If session is expiered anc user is trying to open the 'Quick Setup' modal => go to login state
            if (quickSetup && !this.User.getCurrent().isAdmin()) {
                defer.reject();
                this.ArtifactoryState.removeState('onboardingWizardOpen');
                this.$state.go("login");
                return;
            }
            this.FooterDao.get().then((footerData)=> {
                if (footerData.haConfigured) {
                    this.haConfigured = true;
                }
                defer.resolve();
            });
        });
        let whenReady = defer.promise;

        this.ArtifactoryState.setState('onboardingWizardOpen', true)

        whenReady.then(()=> {
            this.user = this.User.getCurrent();
            let steps = [];
            let icons = [];
            let iconsPreloadObj = [];
            var wizardDefinitionObject = {
                title: 'Quick Setup Wizard',
                controller: WizardController,
                controllerAs: 'WizCtrl',
                cancelable: true,
                backdropCancelable: quickSetup ? true : false,
                steps: steps,
                icons: icons,
                iconsPreload: iconsPreloadObj,
                modalOptions: {keyboard: true}
            };

            if (!quickSetup) steps.push({
                name: 'Welcome to JFrog Artifactory!',
                id: 'welcome',
                templateUrl: 'modal_templates/onboarding_wizard/welcome.html',
                skippable: false,
                supportReturnTo: true,
                class: 'welcome'
            });

            if (!quickSetup && !this.initStatus.hasLicenseAlready && !this.features.isAol() && !this.features.isOss()) steps.push({
                name: 'Activate Your Artifactory Instance',
                id: 'license',
                icon: 'images/wizard-icons/icon_1_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_1_idle_gif@2.gif 2x, images/wizard-icons/icon_1_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_1_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_1_build_gif@2.gif 2x, images/wizard-icons/icon_1_build_gif@3.gif 3x',
                templateUrl: 'modal_templates/onboarding_wizard/license_registration.html',
                description: `Don't have a license? Start a `+
                             `<a href="https://www.jfrog.com/artifactory/free-trial/" target="_blank" class="nowrap">`+
                                `free trial <i class="icon icon-external-link"></i>`+
                             `</a>`,
                skippable: false,
                supportReturnTo: false
            });

            if (!quickSetup && !this.user.isAdmin() && this.initStatus.hasPriorLogins) steps.push({
                name: 'Admin Login',
                id: 'login',
                icon: 'images/wizard-icons/icon_2_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_2_idle_gif@2.gif 2x, images/wizard-icons/icon_2_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_2_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_2_build_gif@2.gif 2x, images/wizard-icons/icon_2_build_gif@3.gif 3x',
                description: !this.features.isAol() ? `Use your admin credentials to login.<br>Didn’t change your default password yet? Do it once you complete this wizard! (you won’t regret it)` : `Log in using the admin credentials you received in the activation email.`,
                templateUrl: 'modal_templates/onboarding_wizard/login.html',
                skippable: false,
                supportReturnTo: false
            });

            if (!quickSetup && !this.user.isAdmin() && !this.initStatus.hasPriorLogins) steps.push({
                name: 'Set Admin Password',
                id: 'setNewPassword',
                icon: 'images/wizard-icons/icon_2_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_2_idle_gif@2.gif 2x, images/wizard-icons/icon_2_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_2_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_2_build_gif@2.gif 2x, images/wizard-icons/icon_2_build_gif@3.gif 3x',
                description: `This new password is for the default admin user.<br>`+
                             `Want to skip? Find the default admin credentials in the `+
                             `<a href="https://www.jfrog.com/confluence/display/RTF/Installing+Artifactory#InstallingArtifactory-DefaultAdminUser" target="_blank" class="nowrap">`+
                                `JFrog Artifactory User Guide <i class="icon icon-external-link"></i>`+
                             `</a>.`,
                templateUrl: 'modal_templates/onboarding_wizard/set_password.html',
                skippable: true,
                supportReturnTo: true
            });

            if (!quickSetup && !this.features.isAol() && !this.initStatus.hasProxies) steps.push({
                name: 'Configure a Proxy Server',
                id: 'proxy',
                icon: 'images/wizard-icons/icon_3_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_3_idle_gif@2.gif 2x, images/wizard-icons/icon_3_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_3_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_3_build_gif@2.gif 2x, images/wizard-icons/icon_3_build_gif@3.gif 3x',
                description: 'This lets you access resources remotely.',
                templateUrl: 'modal_templates/onboarding_wizard/proxy.html',
                skippable: true,
                supportReturnTo: true
            });

            let btns = [
                {label: 'Back', action: 'back'},
                {label: 'Skip', action: 'skip'},
                {label: 'Create', action: 'next'}
            ];
            if (quickSetup) btns.splice(0, 2);

            steps.push({
                name: 'Create Repositories',
                id: 'packageTypes',
                icon: 'images/wizard-icons/icon_4_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_4_idle_gif@2.gif 2x, images/wizard-icons/icon_4_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_4_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_4_build_gif@2.gif 2x, images/wizard-icons/icon_4_build_gif@3.gif 3x',
                description: `Select the package type(s) you want - we’ll create the default repositories for you!<br>`+
                              `Need to skip? No worries, create and manage repositories anytime.`,
                templateUrl: 'modal_templates/onboarding_wizard/package_types.html',
                skippable: true,
                supportReturnTo: false,
                customButtons: btns
            });
            steps.push({
                name: 'Artifactory on-boarding complete!',
                id: 'summary',
                icon: 'images/wizard-icons/icon_5_idle_gif.gif',
                iconSrcset: 'images/wizard-icons/icon_5_idle_gif@2.gif 2x, images/wizard-icons/icon_5_idle_gif@3.gif 3x',
                buildIcon: 'images/wizard-icons/icon_5_build_gif.gif',
                buildIconSrcset: 'images/wizard-icons/icon_5_build_gif@2.gif 2x, images/wizard-icons/icon_5_build_gif@3.gif 3x',
                description: `Congrats! These are the default repositories we created for you.<br>`+
                `You’re now ready to speed up your software releases!<br>`+
                `Want to configure your client(s) and get started? Click the `+
                `<a href="https://www.jfrog.com/confluence/display/RTF/Using+Artifactory#UsingArtifactory-SetMeUp" target="_blank">`+
                    `Set Me Up <i class="icon icon-external-link"></i>`+
                `</a> button for each repository.<br>`+
                `Want to learn more about different repository types? Consult the `+
                `<a href="https://www.jfrog.com/confluence/display/RTF" target="_blank" class="nowrap">`+
                    `JFrog Artifactory User Guide <i class="icon icon-external-link"></i>`+
                `</a>.`,
                templateUrl: 'modal_templates/onboarding_wizard/summary.html',
                skippable: false,
                supportReturnTo: false,
                hideTitleBorder: true
            });

            if (quickSetup) {
                let step = _.find(steps, {id: "summary"});
                step.name = "All Done!";
                step.description = `These are the default repositories we created for you.<br>`+
                                   `Want to configure your client(s) and get started? Click the <a href="https://www.jfrog.com/confluence/display/RTF/Using+Artifactory#UsingArtifactory-SetMeUp" target="_blank">Set Me Up</a> button for each repository.`;
            }

            _.forEach(steps, (step) => {
                // setup the icons object
                let iconData = {}
                if (step.icon) iconData.icon = step.icon;
                if (step.iconSrcset) iconData.iconSrcset = step.iconSrcset;
                if (step.buildIcon) iconData.buildIcon = step.buildIcon;
                if (step.buildIconSrcset) iconData.buildIconSrcset = step.buildIconSrcset;

                if (!_.isEmpty(iconData)) icons.push(iconData);

                // setup flat object of all icons for preloading in the onboarding welcome screen
                if (step.icon && !step.iconSrcset) iconsPreloadObj.push({icon: step.icon})
                if (step.icon && step.iconSrcset) iconsPreloadObj.push({icon: step.icon, iconSrcset: step.iconSrcset})
                if (step.buildIcon && step.buildIconSrcset) iconsPreloadObj.push({icon: step.buildIcon, iconSrcset: step.buildIconSrcset});

            })

            this.modal.launchWizard(wizardDefinitionObject);
        })
    }

    setInitStatus() {
        this.OnboardingDao.initStatus({$no_spinner:true}).$promise.then((status)=> {
            this.ArtifactoryState.setState('initStatus', status);
        });
    }

    isSystemOnboarding() {
        let defer = this.$q.defer();

        if (this.initPending) {
            defer.reject();
            return defer.promise;
        }

        let prevOnboarding = this.ArtifactoryState.getState('onboarding')

        if (prevOnboarding === undefined) {
            this.initPending = true;
            this.OnboardingDao.initStatus({$no_spinner:true}).$promise.then((status)=> {
                this.initPending = false;
                this.initStatus = status;
                if (!this.initStatus.hasPriorLogins || !this.initStatus.hasLicenseAlready) {
                    //This is the first time the wizard appear, so we clean the skipOnboarding flag from localStorage (if it's present)
                    if (localStorage.skipOnboarding) delete localStorage.skipOnboarding;
                }
                this.ArtifactoryState.setState('initStatus', status);
                let onboarding = !this.initStatus.hasRepos && !this.initStatus.skipWizard && !localStorage.skipOnboarding;
                this.ArtifactoryState.setState('onboarding', onboarding)
                defer.resolve(onboarding);
            }).catch(()=>{
                this.initPending = false;
                this.FooterDao.get().then(()=>{
                    if (this.features.isAol()) {
                        this.ArtifactoryState.setState('aolOnboarding', true)
                    }
                    this.ArtifactoryState.setState('onboarding', false)
                    defer.resolve(false);
                })
            })
        }
        else {
            defer.resolve(prevOnboarding);
        }

        return defer.promise;
    }

}


class WizardController {
    constructor($timeout, $q, ArtifactoryState, ArtifactoryStorage, User, RegisterProDao, JFrogEventBus, ProxiesDao,
                OnboardingDao, UserDao, FooterDao, SaveArtifactoryHaLicenses, GoogleAnalytics) {
        this.ArtifactoryState = ArtifactoryState;
        this.ArtifactoryStorage = ArtifactoryStorage;
        this.ProxiesDao = ProxiesDao;
        this.UserService = User;
        this.UserDao = UserDao.getInstance();
        this.registerProDao = RegisterProDao;
        this.JFrogEventBus = JFrogEventBus;
        this.$timeout = $timeout;
        this.$q = $q;
        this.OnboardingDao = OnboardingDao;
        this.footerDao = FooterDao;
        this.saveArtifactoryHaLicenses = SaveArtifactoryHaLicenses;
        this.GoogleAnalytics = GoogleAnalytics

        this.login = {};
        this.setPassword = {passwordRank: 0};
        this.license = {};
        this.proxy = {
            defaultProxy: true,
            systemDefault: TOOLTIP.admin.configuration.proxyForm.systemDefault,
            redirectingProxyTargetHosts: TOOLTIP.admin.configuration.proxyForm.redirectingProxyTargetHosts
        };
        this.packageTypes = {};

        if (this.UserService.getCurrent().isAdmin()) this.initPackageTypesStep();

    }

    initPackageTypesStep() {
        this.hasAlreadySetPackages = false;
        if (!this.gotPackageTypes) {
            this.OnboardingDao.reposStates().$promise.then((response)=> {
                this.packageTypes.packageTypes = _.cloneDeep(fieldOptions.repoPackageTypes);
                this.packageTypes.packageTypes = _.map(this.packageTypes.packageTypes, (type)=> {
                    let id = type.serverEnumName;
                    let state = response.repoStates[id];
                    if (state === 'ALREADY_SET') {
                        type.disabled = true;
                        this.hasAlreadySetPackages = true;
                        type.tooltip = "Default repositories already configured";
                    }
                    if (state === 'UNAVAILABLE') type.unavailable = true;

                    // Technology name below icons
                    let addTextBelowIcon = ['Bower', 'Chef', 'CocoaPods', 'Conan', 'Pypi', 'Puppet', 'Opkg', 'Composer', 'SBT', 'Gradle', 'Gems', 'NuGet', 'GitLfs','Generic'];
                    if (_.includes(addTextBelowIcon, type.serverEnumName)) type.helpText = true;
                    if (!_.includes(['UNSET', 'ALREADY_SET', 'UNAVAILABLE'], state)) type.ignore = true;

                    return type;
                });

                if (this.hasAlreadySetPackages) {
                    let step = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "packageTypes"});
                    step.description = `Select the package type(s) you want - we’ll create the default repositories for you!<br>Disabled package types already have default repositories configured.`;
                }

                this.packageTypes.packageTypes = _.sortBy(this.packageTypes.packageTypes, (type) => type.text.toLowerCase());
                this.packageTypes.packageTypes = _.sortBy(this.packageTypes.packageTypes, (type)=>(!type.unavailable && !type.disabled) ? 0 : type.disabled ? 1 : 2);

                // Change PHP Composer -> Composer
                _.find(this.packageTypes.packageTypes, (type) => {
                    if (type.serverEnumName === 'Composer') {
                        type.text = 'Composer';
                    }
                });


                this.gotPackageTypes = true;
            });
        }
        this.packageTypes.highlightCheck = (typeFilter, type) => {
            if (type.selected) return true;

            if (typeFilter) {
                let string = type.text.toLowerCase(),
                    searchstring = typeFilter.toLowerCase().replace(/ /g, '');


                if (string.substr(0, searchstring.length) == searchstring) {
                    type.highlighted = true;
                    return true;
                } else {
                    type.highlighted = false;
                    return false;
                }
            }
        };
        this.packageTypes.checkNoResults = (typeFilter) => {
            if (typeFilter && typeFilter.length > 0 && _.filter(this.packageTypes.packageTypes, (type)=>type.highlighted).length == 0) {
                return true;
            }
        };
        this.packageTypes.selectRepoType = (type) => {
            type.selected = !type.selected;
            this.packageTypes.selectedTypes = _.filter(this.packageTypes.packageTypes, (type)=>type.selected);
        }
    }

    isStepCompleted(step) {
        switch (step.id) {
            case 'login': {
                return this.loginForm && this.loginForm.$valid;
            }
            case 'setNewPassword': {
                return this.setPasswordForm && this.setPasswordForm.$valid && this.setPassword.newPassword === this.setPassword.retypeNewPassword;
            }
            case 'license': {
                return this.licenseForm && this.licenseForm.$valid && this.licensesTextBoxHasText();
            }
            case 'proxy': {
                return this.proxyForm && this.proxyForm.$valid;
            }
            case 'packageTypes': {
                return this.packageTypes.selectedTypes && this.packageTypes.selectedTypes.length;
            }
            case 'summary': {
                return true;
            }
        }
        return true;
    }

    licensesTextBoxHasText(){
        return this.license.key != "" &&
               this.license.key != 'undefined' &&
               this.license.key != null;
    }

    onComplete() {
        this.ArtifactoryState.setState('onboarding', false);
        delete localStorage.fakeOnboarding;

        this.ArtifactoryState.setState('onboardingWizardOpen', false)

        if (this.skippedTechs && !this.quickSetup) {
            this.packageTypes.selectedTypes = [];
            this.sendPackageTypes();
            this.ArtifactoryStorage.setItem('skipOnboarding', true);
        }

        this.JFrogEventBus.dispatch(EVENTS.REFRESH_PAGE_CONTENT);
    }

    onCancel() {
        //let isOnboarding = this.ArtifactoryState.getState('onboarding');

        // stop animation ticker, if still running
        createjs.Ticker.removeAllEventListeners();

        let isSummary = this.$wizardCtrl.wizardDefinitionObject.steps[this.$wizardCtrl.currentStep-1].id === 'summary';

        // track close or esc wizard with the step the user closed the modal
        if (!this.quickSetup && !isSummary) {
            this.GoogleAnalytics.trackEvent('Onboarding Wizard' , 'close/esc wizard' , this.$wizardCtrl.wizardDefinitionObject.steps[this.$wizardCtrl.currentStep-1].id)
        } else if (!isSummary) {
            this.GoogleAnalytics.trackEvent('Homepage' , 'close/esc quick setup' , this.$wizardCtrl.wizardDefinitionObject.steps[this.$wizardCtrl.currentStep-1].id)
        }

        this.ArtifactoryState.setState('onboardingWizardOpen', 'canceled');

        // Fire refresh event in onCancel() call only of this is not the quick setup
        if(!this.quickSetup){
            this.JFrogEventBus.dispatch(EVENTS.REFRESH_PAGE_CONTENT);
        }
    }

    onWizardShow(newStep) {
        $('.title-wrapper').addClass('icon-hidden');
        if (newStep.buildIcon) {
            this.$timeout(() => {
                $('.title-wrapper .build').show();
                this.$timeout(() => {
                    $('.title-wrapper .build').attr({
                        'src' : newStep.buildIcon,
                        'srcset' : newStep.buildIconSrcset
                    });
                    $('.title-wrapper').removeClass('icon-hidden');
                }, 100);
                this.$timeout(() => $('.title-wrapper .build').hide(), 1400);
            });
        } else {
            this.$timeout(() => {
                if ('.icon-hidden') $('.title-wrapper').removeClass('icon-hidden');
            });
        }
    }
    afterStepChange(newStep, oldStep, reason) {
        $('.title-wrapper').addClass('icon-hidden');
        if (newStep.buildIcon) {
            this.$timeout(() => {
                $('.title-wrapper .build').show();
                this.$timeout(() => {
                    $('.title-wrapper .build').attr({
                        'src' : newStep.buildIcon,
                        'srcset' : newStep.buildIconSrcset
                    });
                    $('.title-wrapper').removeClass('icon-hidden');
                }, 50);
                this.$timeout(() => $('.title-wrapper .build').hide(), 1400);
            });
        } else {
            this.$timeout(() => {
                if ('.icon-hidden') $('.title-wrapper').removeClass('icon-hidden');
            });
        }

        this.GoogleAnalytics.trackEvent('Onboarding Wizard' , 'Completed Step - ' + oldStep.id, reason);
    }
    onStepChange(newStep, oldStep, reason) {
        if (oldStep.id === 'welcome') {
            // stop animation ticker
            this.$timeout(()=>createjs.Ticker.removeAllEventListeners(),500)
        }
        else if (oldStep.id === 'login' && reason === 'next') {
            let defer = this.$q.defer();
            this.doLogin(defer);
            return defer.promise;
        }
        else if (oldStep.id === 'setNewPassword' && reason === 'next') {
            let defer = this.$q.defer();
            this.setNewPassword(defer);
            return defer.promise;
        }
        else if (oldStep.id === 'setNewPassword' && reason === 'skip') {
            let defer = this.$q.defer();
            this.login.user = 'admin';
            this.login.password = 'password';
            this.doLogin(defer);
            return defer.promise;
        }
        else if (oldStep.id === 'license' && reason === 'next') {
            let defer = this.$q.defer();
            this.registerLicense(defer);
            return defer.promise;
        }
        else if (oldStep.id === 'proxy' && reason === 'next') {
            let defer = this.$q.defer();
            this.createProxy(defer);
            return defer.promise;
        }
        else if (oldStep.id === 'packageTypes' && reason === 'next') {
            let step = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "summary"});
            if (step.class) delete step.class;

            let defer = this.$q.defer();
            this.sendPackageTypes(defer);
            this.skippedTechs = false;
            return defer.promise;
        }
        else if (oldStep.id === 'packageTypes' && reason === 'skip') {
            let step = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "summary"});
            step.name = `All done!`;
            let prev = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "packageTypes"});
            prev.supportReturnTo = true;
            this.skippedTechs = true;
            step.class = "no-repositories-modal";
        }
        else if (reason === 'prev') {
            this.errorMessage = '';
        }
    }

    doLogin(defer) {

        this.UserService.login(this.login, false).then(success.bind(this), error.bind(this))

        function success(result) {
            if (result.data.admin) {
                this.errorMessage = '';
                defer.resolve();
                this.initPackageTypesStep(); //Needs to be logged in
            }
            else {
                this.errorMessage = 'You must be logged in with an admin user in order to use this wizard';
                this.UserService.logout();
                defer.reject();
            }
        }

        function error(response) {
            if (response.data) {
                this.errorMessage = response.data.error;
            }
            defer.reject();
        }

    }

    setNewPassword(defer) {

        this.UserDao.changePassword({$suppress_toaster: true}, {
            userName: 'admin',
            oldPassword: 'password',
            newPassword1: this.setPassword.newPassword,
            newPassword2: this.setPassword.retypeNewPassword
        }).$promise.then((res)=> {
            if (res.status === 200) {
                let step = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "setNewPassword"});
                step.supportReturnTo = false;
                this.login.user = 'admin';
                this.login.password = this.setPassword.newPassword;
                this.doLogin(defer);
            }
        }).catch((response)=>{
            defer.reject();
            if (response.data && this.setPassword.newPassword === 'password') {
                this.errorMessage = response.data.error;
            }
            else{
                this.errorMessage = '';
            }
        });

    }

    onRegisterLicenses(defer, data) {
        this.JFrogEventBus.dispatch(EVENTS.FOOTER_REFRESH);
        this.UserService.loadUser(true);
        this.errorMessage = '';

        if (data.status === 200) {
            let initStatus = this.ArtifactoryState.getState('initStatus');
            if (initStatus) initStatus.hasLicenseAlready = true;
            defer.resolve();
        }
        else {
            defer.reject();
        }
    }

    removeComments(text){
        return text.replace(/#+((?:.)+?)*/g,'');
    }

    registerLicense(defer) {
        let promise;
        this.license.key = this.removeComments(this.license.key);

        if (this.mainCtrl.haConfigured) {
            promise = this.saveArtifactoryHaLicenses.saveLicenses({$suppress_toaster: true}, this.license.key);
        }
        else {
            promise = this.registerProDao.update({$suppress_toaster: true}, this.license).$promise;
        }

        promise.then((data)=> {
            this.onRegisterLicenses(defer, data);
        }).catch((res)=> {
            this.errorMessage = res.data.error;

            defer.reject();
        });
    }

    onError(errorMessage){
        this.clearErrorMessage();
        this.errorMessage = errorMessage;
        this.$timeout(()=>{
            $('.wizard-modal').focus();
        });
    }

    createProxy(defer) {
        this.ProxiesDao[this.proxySaved ? 'update' : 'save']({$suppress_toaster: true}, this.proxy).$promise.then(() => {
            defer.resolve();
            this.proxySaved = true
        }).catch(()=>defer.reject());
    }

    sendPackageTypes(defer) {
        let payload = {
            repoTypeList: _.pluck(this.packageTypes.selectedTypes, 'serverEnumName'),
            fromOnboarding: !this.quickSetup
        };

        this.OnboardingDao.createDefaultRepos(payload).$promise.then((response)=> {
            if (!this.quickSetup) {
                this.GoogleAnalytics.trackEvent('Onboarding Wizard' , !this.packageTypes.selectedTypes.length ? 'Wizard completed - no repos' : 'Wizard completed + repos', _.pluck(this.packageTypes.selectedTypes, 'text').toString() , this.packageTypes.selectedTypes.length);
            } else {
                this.GoogleAnalytics.trackEvent('Homepage' , 'Quick repository setup completed + repos', _.pluck(this.packageTypes.selectedTypes, 'text').toString() , this.packageTypes.selectedTypes.length)
            }

            this.summary = {data: response.createdReposByTypes};
            let step = _.find(this.$wizardCtrl.wizardDefinitionObject.steps, {id: "packageTypes"});
            step.supportReturnTo = false;
            this.JFrogEventBus.dispatch(EVENTS.REFRESH_SETMEUP_WIZARD);
            if (defer) defer.resolve();
        }).catch(()=> {
            if (defer) defer.reject()
        });
    }

    getIconForPackageType(type) {
        let packageData = _.find(this.packageTypes.packageTypes, {value: type});
        return packageData ? packageData.icon : '';
    }

    initMachine() {
        var canvas, stage, exportRoot;

        canvas = document.getElementById("canvas");
        exportRoot = new lib.jfrog_v45_Html_5();

        stage = new createjs.Stage(canvas);
        stage.addChild(exportRoot);
        stage.update();

        createjs.Ticker.setFPS(lib.properties.fps);
        createjs.Ticker.addEventListener("tick", stage);
    }

    clearErrorMessage(){
        this.errorMessage = '';
    }
}

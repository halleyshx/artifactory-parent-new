import EVENTS from "../constants/artifacts_events.constants";
const USER_KEY = 'USER';
const GUEST_USER = {
    name: 'anonymous',
    admin: false,
    profileUpdatable: true,
    internalPasswordDisabled: false,
    canDeploy: false,
    canManage: false,
    preventAnonAccessBuild: false,
    proWithoutLicense: false
};

class User {
    constructor(data) {
        User.JFrogEventBus.register(EVENTS.USER_LOGOUT, (confirmDiscard) => {
            if (!confirmDiscard) {
                User.logout().then(() => {
                    User.$state.go("home");
                });
            }
            else if (confirmDiscard === "logoutAndLogin") {
                User.logout();
            }
        });

        if (data) {
            this.setData(data);
        }

        this.setIsHa();
    }

    setData(data) {
        if (!_.isEqual(this._data, data)) {
            data.userPreferences = data.userPreferences || {};

            angular.copy(data, this);
            this._data = data;
            User.JFrogEventBus.dispatch(EVENTS.USER_CHANGED);
        }
        User.footerDao.get(false).then(footerData => this.footerData = footerData);
    }

    isProWithoutLicense() {
        return this.proWithoutLicense;
    }

    // Instance methods:
    isGuest() {
        return this.name === GUEST_USER.name;
    }

    isAdmin() {
        return this.admin;
    }

    isRegularUser() {
        return this.isLoggedIn() && !this.isAdmin();
    }

    isLoggedIn() {
        return !this.isGuest();
    }

    getCanManage() {
        return this.canManage || this.isProWithoutLicense();
    }

    getCanDeploy() {
        if (this.isProWithoutLicense()) {
            return false
        }
        return this.canDeploy;
    }

    setIsHa(){
        User.footerDao.get(false).then(footerData => {
            this.isHaConfigured = footerData.haConfigured;
        });
    }


    haLicenseInstalled() {
        return (this.isHaConfigured && !this.isProWithoutLicense());
    }

    //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
    /*canPushToBintray() {
     if (this.isProWithoutLicense()) {
     return false
     }
     return this.canDeploy;
     }*/

    canViewBuildState(state, stateParams, isChangeTab) {

        if (this.isProWithoutLicense()) {
            return false;
        }
        if (this.preventAnonAccessBuild && this.isGuest()) {
            return false;
        }
        if (state != 'builds.info') {
            return true;
        }

        if (stateParams.tab === 'published') {
            return true;
        }
        return this.getCanDeploy();
    }

    canView(state, stateParams = {}) {

        // High Availability configured and the user is not admin and the master node has a license installed
        if (this.haLicenseInstalled() && !this.isAdmin()){
            if (state === "admin.configuration.register_pro"){
                return false;
            }
        }

        if (this.isProWithoutLicense()) {
            if (state === "admin.configuration.register_pro" || state === "admin.configuration" || state === "admin" ||
                    state === "home" || state === "login") {
                return true;
            } else {
                return false;
            }
        }

        if (state === "artifacts") {
            return true;
        }
        if (state.match(/^admin.security.permissions/) || state === "admin") {
            return this.getCanManage();
        }
        else if (state.match(/^admin/)) {
            return this.isAdmin();
        }
        else if (state.match(/^builds/)) {
            return this.canViewBuildState(state, stateParams, true);
        }
        else {
            return true;
        }
    }

    // Class methods:
    static login(username, remember) {
        let loginRequest = this.http.post(this.RESOURCE.AUTH_LOGIN + remember,
                angular.extend(username, {type: 'login'}));

        loginRequest.then(
                (response) => {
                    this.setUser(response.data);
                    User.$timeout(()=>User.JFrogEventBus.dispatch(EVENTS.FOOTER_REFRESH));
                    return username;
                });
        return loginRequest;
    }

    static logout() {
        return this.http.get(this.RESOURCE.AUTH_IS_SAML, null, {}).then((res=> {
            if (res.data) {
                return this.http.get(this.RESOURCE.SAML_LOGOUT, null, {}).then((res)=> {
                    this.$window.location.replace(res.data);
                });
            }
            else {
                return this.http.post(this.RESOURCE.AUTH_LOGOUT, null, {bypassSessionInterceptor: true})
                        .then((res) => {
                            let isOnboarding = this.artifactoryState.getState('onboardingWizardOpen') === true;

                            if (!isOnboarding) {
                                this.clearStates();
                            }


                            if (this.$state.current.name === 'home' && isOnboarding !== true) {
                                this.$state.go(this.$state.current, this.$stateParams, {reload: true});
                            }

                            return this.loadUser(true);

                        });
            }
        }));
    }

    static clearStates() {
        // save some states we want to keep
        let tempStates = this._getStates(['systemMessage','clearErrorsOnStateChange','sidebarEventsRegistered'])
        // clear the states
        this.artifactoryState.clearAll();
        // restore saved states
        this._setStates(tempStates);
    }

    static _getStates(states) {
        let savedStates = {};
        states.forEach(s=>{
            savedStates[s] = this.artifactoryState.getState(s);
        })
        return savedStates;
    }
    static _setStates(savedStates) {
        for (let key in savedStates) {
            this.artifactoryState.setState(key, savedStates[key]);
        }
    }

    static forgotPassword(user) {
        return this.http.post(this.RESOURCE.AUTH_FORGOT_PASSWORD, user);
    }

    static validateKey(key) {
        return this.http.post(this.RESOURCE.AUTH_VALIDATE_KEY + key);
    }

    static resetPassword(key, user) {
        return this.http.post(this.RESOURCE.AUTH_RESET_PASSWORD + key, user);
    }

    static canAnnotate(repoKey, path) {
        return this.http.get(this.RESOURCE.AUTH_CAN_ANNOTATE + repoKey + '&path=' + path).then((response) => {
            return response;
        });
    }

    static getLoginData() {
        return this.http.post(this.RESOURCE.AUTH_LOGIN_DATA).then((response) => {
            return response.data;//!!response.data.forgotPassword;
        });
    }

    static getOAuthLoginData() {
        return this.http.get(this.RESOURCE.OAUTH_LOGIN).then((response) => {
            return response.data;
        });
    }

    static setUser(user) {
        this.currentUser.setData(user);
        this.storage.setItem(USER_KEY, user);
        return this.currentUser;
    }

    static loadUser(force = false) {
        var user = this.storage.getItem(USER_KEY);
        if (user) {
            this.currentUser.setData(user);
        }
        if (force || !user) {
            this.whenLoadedFromServer =
                    this.http.get(this.RESOURCE.AUTH_CURRENT, {bypassSessionInterceptor: true}).then((user) => {
                        return this.setUser(user.data)
                    });
            return this.whenLoadedFromServer;
            /*

             this.whenLoadedFromServer = this.http.get(this.RESOURCE.AUTH_CURRENT, {bypassSessionInterceptor: true})
             .then((user) => this.setUser(user.data));
             return this.whenLoadedFromServer;

             */
        }
        else {
            return this.$q.when(this.currentUser)
        }
    }

    static getCurrent() {
        return this.currentUser;
    }

    static reload() {
        this.loadUser(true);
    }
}


export function UserFactory(ArtifactoryHttpClient, ArtifactoryStorage, RESOURCE, $q, $window, $state, $timeout,
        $stateParams, JFrogEventBus, ArtifactoryFeatures,
        ArtifactoryState, FooterDao) {
    // Set static members on class:
    User.http = ArtifactoryHttpClient;
    User.storage = ArtifactoryStorage;
    User.RESOURCE = RESOURCE;
    User.$q = $q;
    User.$window = $window;
    User.$timeout = $timeout;
    User.$state = $state;
    User.$stateParams = $stateParams;
    User.artifactoryState = ArtifactoryState;
    User.JFrogEventBus = JFrogEventBus;
    User.footerDao = FooterDao;
    User.currentUser = new User();
    // Load user from localstorage:
    User.loadUser(/* force */ true);

    return User;
}

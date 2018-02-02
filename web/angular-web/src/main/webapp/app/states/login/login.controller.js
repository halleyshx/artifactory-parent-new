import EVENTS from '../../constants/artifacts_events.constants';
export class LoginController {

    constructor($state, $timeout, FooterDao, $stateParams, User, $location, $window, ArtifactoryState, JFrogEventBus,
                JFrogNotifications, ArtifactoryFeatures, OnBoardingWizard) {
        this.user = {};
        this.rememberMe = false;
        this.UserService = User;
        this.$state = $state;
        this.$window = $window;
        this.JFrogEventBus = JFrogEventBus;
        this.artifactoryNotifications = JFrogNotifications;
        this.$location = $location;
        this.ArtifactoryState = ArtifactoryState;
        this.ArtifactoryFeatures = ArtifactoryFeatures;
        this.OnBoardingWizard = OnBoardingWizard;
        this.FooterDao = FooterDao;
        this.$timeout = $timeout;
        this.canResetPassword = false;
        this.canRememberMe = false;
        this.loginForm = null;
        this.pending = false;

        this.EVENTS = JFrogEventBus.getEventsDefinition();


        this.canExit = (User.currentUser.name !== 'anonymous' || User.currentUser.anonAccessEnabled);

        this.oauth = {}
        User.getOAuthLoginData().then((response) => {
            this.oauth.providers = response;
        });

        if ($stateParams.oauthError) this.errorMessage = $stateParams.oauthError;

        this.checkResetPassword();
    }

    login() {

        this.JFrogEventBus.dispatch(this.EVENTS.FORM_SUBMITTED);

        if (this.loginForm.$valid && !this.pending) {
            this.pending = true;
            this.UserService.login(this.user, this.rememberMe).then(success.bind(this), error.bind(this))
        }

        function success(result) {
            this.pending = false;
            let urlAfterLogin = this.ArtifactoryState.getState('urlAfterLogin');
            let aolOnboarding = this.ArtifactoryState.getState('aolOnboarding');

            this.ArtifactoryState.setState('clearErrorsOnStateChange',true)

            if (aolOnboarding) {
                this.ArtifactoryState.setState('onboarding', undefined);
                this.OnBoardingWizard.isSystemOnboarding().then((onBoarding) => {
                    if (onBoarding) {
                        this.$state.go('home').then(() =>  this.OnBoardingWizard.show());
                    }
                    else {
                        this.changeUrlWhenNotOnboarding(urlAfterLogin);
                    }
                });
            }
            else {
                this.OnBoardingWizard.setInitStatus();
                this.changeUrlWhenNotOnboarding(urlAfterLogin);
            }
        }

        function error(response) {
            this.pending = false;
            if (response.data) {
                if (!this.catchExpired(response)) {
                    this.errorMessage = response.data.error;
                }
            }
        }

    }

    changeUrlWhenNotOnboarding(urlAfterLogin){
        if (urlAfterLogin) {
            this.$location.path(urlAfterLogin)
        }
        else {
            this.$state.go('home');
        }
    }

    /**
     * Check if already logged in
     * */
    isLoggedIn(){
        return this.UserService.getCurrent().isLoggedIn();
    }

    /**
     * Logout is dispatching an event.
     * The handler also checks if the current state is one of the admin states.
     * This is done in order to make sure that logout happens only after all open admin states (windows) are closed.
     * Otherwise the user could be stuck with an unresponsive screen.
     * */
    logOut(){
        this.JFrogEventBus.dispatch(EVENTS.USER_LOGOUT,"logoutAndLogin");
    }

    /**
     * Go to home state
     * */
    goHome(){
        this.UserService.$state.go("home");
    }

    catchExpired(response) {
        let code = 'CREDENTIALS_EXPIRED';
        if (response.data && response.data.code && response.data.code === code) {
            let msg = response.data.feedbackMsg.error;
            if (response.data.profileUpdatable) {
                this.$state.go('change-password', {username: this.user.user});
            }
            else {
                msg += '.\nPlease contact your system administrator.'
                this.artifactoryNotifications.create({error: msg});
            }
            return true;
        }
        return false;
    }

    userPasswordChanged() {
        this.errorMessage = null;
    }

    checkResetPassword() {
        this.UserService.getLoginData().then((response) => {
            this.canResetPassword = response.forgotPassword;
            this.canRememberMe = response.canRememberMe;
            this.ssoProviderLink = response.ssoProviderLink;
            this.oauthProviderLink = response.oauthProviderLink;
        });
    }

    gotoForgotPwd() {
        this.$state.go('forgot-password');
    }

/*
    oauthLogin() {
        this.$window.open(this.oauthProviderLink,'_self');
    }
*/

    ssoLogin() {
        this.$window.open(this.ssoProviderLink,'_self');
    }

    isOAuthEnabled() {
        return this.oauth.providers && this.oauth.providers.length > 0;
    }

    onGotoOAuth() {
        localStorage.stateBeforeOAuth = this.$state.current.name;
    }
}

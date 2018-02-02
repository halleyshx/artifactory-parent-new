window._sessionExpire = function () {
    localStorage._forceSessionExpire = true;
}

export function artifactorySessionInterceptor($injector) {
    var User;
    var $state;
    var $stateParams;
    var ArtifactoryState;
    var $location;
    var RESOURCE;
    var $q;
    var JFrogNotifications;
    var ArtifactoryHttpClient;
    var $window;

    function initInjectables() {
        $q = $q || $injector.get('$q');
        $window = $window || $injector.get('$window');
        User = User || $injector.get('User');
        $state = $state || $injector.get('$state');
        $stateParams = $stateParams || $injector.get('$stateParams');
        ArtifactoryState = ArtifactoryState || $injector.get('ArtifactoryState');
        $location = $location || $injector.get('$location');
        RESOURCE = RESOURCE || $injector.get('RESOURCE');
        JFrogNotifications = JFrogNotifications || $injector.get('JFrogNotifications');
        ArtifactoryHttpClient = ArtifactoryHttpClient || $injector.get('ArtifactoryHttpClient');
    }

    function bypass(res) {
        return res.config && res.config.bypassSessionInterceptor;
    };

    function isSessionInvalid(res) {
        return res.headers().sessionvalid === "false";
    }

    function isApiRequest(res) {
        return _.contains(res.config.url, RESOURCE.API_URL);
    }
    function isOpenApi(res) {
        return isApiRequest(res) && (res.config.url.endsWith('/auth/current') || res.config.url.endsWith('/auth/screen/footer'));
    }
    function isLoggedIn() {
        return !User.getCurrent().isGuest();
    }

    function handleExpiredSession() {
        // if session invalid and we think we are logged in - session expired on server
        delete localStorage._forceSessionExpire;
        User.loadUser(true);

        if ($state.current !== 'login' && $location.path() !== '/login') {
            setUrlAfterLogin();
        }
        return true;
    }

    function verifySession(res) {
        initInjectables();
        if (bypass(res)) {
            return true;
        }

        User.loadUser(); // Refresh from localstorage (parallel tab support)
        if (isApiRequest(res) && !isOpenApi(res) && isSessionInvalid(res) && isLoggedIn() || localStorage._forceSessionExpire) {
            // if the user is not logged in but is in a bypassed request
            // let the request go through but log out the user.
            if ($location.path() !== '/login'){
                setUrlAfterLogin();
            }
            return handleExpiredSession();
        }
        return true;
    }

    function checkAuthorization(res) {
        if (res.status === 401) {
            ArtifactoryHttpClient.post("/auth/loginRelatedData", null,{}).then((res)=>{
               if(res.data.ssoProviderLink) {
                   if ($location.path() == '/login') {
                       reloadUserAndChangeState('login');
                   } else {
                        $window.open(res.data.ssoProviderLink, "_self");
                   }
               } else {
                   if ($state.current.name !== 'reset-password' || !$stateParams.key) {
                       if ($state.current !== 'login' && $location.path() !== '/login'
                               && $state.current !== 'reset-password' && $location.path() !== '/resetpassword') {
                           setUrlAfterLogin();
                       }
                       reloadUserAndChangeState('login');
                   }
               }
            });
        }
        else if (res.status === 403) {
            if (res.config.url.indexOf('targetPermissions') !== -1) {
                JFrogNotifications.create({error: 'You are not authorized to view this page'});
                $state.go('home');
            }
        }
    }

    // Reloading user after receiving a 401 is necessary.
    // Otherwise the user would not be considered as logged in by the UI.
    function reloadUserAndChangeState(toState){
        User.loadUser(true).then(()=>$state.go(toState));
    }

    function setUrlAfterLogin() {
        ArtifactoryState.setState('urlAfterLogin', $location.path());
    }

    function response(res) {
        if (verifySession(res)) {
            return res;
        }
        else {
            return $q.reject(res);
        }
    }

    function responseError(res) {
        verifySession(res);
        checkAuthorization(res);
        return $q.reject(res);
    }

    return {
        response: response,
        responseError: responseError
    };
}

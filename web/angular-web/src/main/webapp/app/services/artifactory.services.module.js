import ApiConstants from "../constants/api.constants";
import {ArtifactoryCookies} from "./artifactory_cookies";
import {ArtifactoryHttpClient} from "./artifactory_http_client";
import {ArtifactoryStorage} from "./artifactory_storage";
import {ArtifactoryXmlParser} from "./artifactory_xml_parser";
import {UserFactory} from "./user";
import {ArtifactoryState} from "./artifactory_state";
import {artifactorySessionInterceptor} from "./artifactory_session_interceptor";
import {artifactoryDebugInterceptor} from "./artifactory_debug_interceptor";
import {artifactorySpinnerInterceptor} from "./artifactory_spinner_interceptor";
import {artifactoryMessageInterceptor} from "./artifactory_message_interceptor";
import {artifactoryServerErrorInterceptor} from "./artifactory_server_error_interceptor";
import {ArtifactoryModelSaverFactory} from "./artifactory_model_saver";
import {ArtifactoryFeatures} from "./artifactory_features";
import {GoogleAnalytics} from "./google_analytics";
import {NativeBrowser} from "./native_browser";
import {ArtifactActions} from "./artifact_actions";
import {SetMeUpModal} from "./set_me_up_modal";
import {ArtifactoryDeployModal} from "./artifactory_deploy_modal";
import {PushToBintrayModal} from "./push_to_bintray_modal.js";
import {parseUrl} from "./parse_url";
import {recursiveDirective} from "./recursive_directive";
import {AdvancedStringMatch} from "./advanced_string_match";
import {ArtifactorySidebarDriver} from "./artifactory_sidebar_driver";
import {OnBoardingWizard} from "./onboarding_wizard";
import {SaveArtifactoryHaLicenses} from './save_artifactory_ha_licenses';

//import {artifactoryIFrameDownload}              from './artifactory_iframe_download';

angular.module('artifactory.services', ['ui.router', 'artifactory.ui_components', 'toaster'])
        .constant('RESOURCE', ApiConstants)
        .service('ArtifactoryCookies', ArtifactoryCookies)
        .service('ArtifactoryHttpClient', ArtifactoryHttpClient)
        .service('ArtifactoryStorage', ArtifactoryStorage)
        .service('ArtifactoryXmlParser', ArtifactoryXmlParser)
        .service('User', UserFactory)
        .service('ArtifactoryState', ArtifactoryState)
        //        .factory('artifactoryIFrameDownload', artifactoryIFrameDownload)
        .factory('artifactorySessionInterceptor', artifactorySessionInterceptor)
        .factory('artifactoryDebugInterceptor', artifactoryDebugInterceptor)
        .factory('artifactoryMessageInterceptor', artifactoryMessageInterceptor)
        .factory('artifactoryServerErrorInterceptor', artifactoryServerErrorInterceptor)
        .factory('artifactorySpinnerInterceptor', artifactorySpinnerInterceptor)
        .service('NativeBrowser', NativeBrowser)
        .service('ArtifactoryFeatures', ArtifactoryFeatures)
        .service('GoogleAnalytics', GoogleAnalytics)
        .service('ArtifactActions', ArtifactActions)
        .service('SetMeUpModal', SetMeUpModal)
        .factory('parseUrl', parseUrl)
        .factory('recursiveDirective', recursiveDirective)
        .factory('ArtifactoryModelSaver', ArtifactoryModelSaverFactory)
        .service('AdvancedStringMatch', AdvancedStringMatch)
        .service('ArtifactoryDeployModal', ArtifactoryDeployModal)
        .service('PushToBintrayModal', PushToBintrayModal)
        .service('ArtifactorySidebarDriver', ArtifactorySidebarDriver)
        .service('OnBoardingWizard', OnBoardingWizard)
        .service('SaveArtifactoryHaLicenses', SaveArtifactoryHaLicenses);
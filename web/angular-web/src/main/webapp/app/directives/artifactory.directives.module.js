import {jfAccordion} from "./jf_accordion/jf_accordion";
import {jfFooter} from "./jf_footer/jf_footer";
import {jfHeader} from "./jf_header/jf_header";
import {jfMessages} from "./jf_messages/jf_messages";
import {jfHeaderSearch} from "./jf_header_search/jf_header_search";
import {jfBrowseFiles} from "./jf_browse_files/jf_browse_files";
import {jfSwitchToggle} from "./jf_switch_toggle/jf_switch_toggle";
import dynamicDirective from "./jf_dynamic_directive/jf_dynamic_directive";
import {jfMultiDeploy} from "./jf_deploy/jf_multi_deploy";
import {jfSingleDeploy} from "./jf_deploy/jf_single_deploy";
import {jfPrint} from "./jf_print/jf_print";
import {jfAutoFocus} from "./jf_autofocus/jf_autofocus";
import {jfBodyClass} from "./jf_body_class/jf_body_class";
import {jfInputTextV2} from "./jf_input_text_v2/jf_input_text_v2";
import {jfCronFormatter} from "./jf_cron_formatter/jf_cron_formatter";
import {jfBreadcrumb} from "./jf_breadcrumb/jf_breadcrumb";
import {jfSpinner} from "./jf_spinner/jf_spinner";
import {jfDisableFeature} from "./jf_disable_feature/jf_disable_feature";
import {jfHideForAol} from "./jf_hide_for_aol/jf_hide_for_aol";
import {jfFileDrop} from "./jf_file_drop/jf_file_drop";
import {jfAutoComplete} from "./jf_auto_complete/jf_auto_complete";
import {jfDockerV2Layer} from "./jf_docker_v2_layer/jf_docker_v2_layer";
import {jfDockerV2Layers} from "./jf_docker_v2_layers/jf_docker_v2_layers";
import {rtfactStorageViewer} from "./rtfact_storage_viewer/rtfact_storage_viewer";
import {rtfactStorageElement} from "./rtfact_storage_viewer/rtfact_storage_element";
import {rtfactStorageUsage} from "./rtfact_storage_viewer/rtfact_storage_usage";
import {jfValidatorName} from "./validators/jf_validator_name";
import {jfValidatorNamePrefixNotNumeric} from "./validators/jf_validator_name_prefix_not_numeric";
import {jfValidatorUniqueId} from "./validators/jf_validator_unique_id";
import {jfValidatorXmlName} from "./validators/jf_validator_xml_name";
import {jfValidatorCron} from "./validators/jf_validator_cron";
import {jfValidatorLdapUrl} from "./validators/jf_validator_ldap_url";
import {jfValidatorPathPattern} from "./validators/jf_validator_path_pattern";
import {jfValidatorIntValue} from "./validators/jf_validator_int_value";
import {jfValidatorMaxTextLength} from "./validators/jf_validator_max_text_length";
import {jfSpecialChars} from "./jf_special_chars/jf_special_chars";
import {jfRepokeyValidator} from "./jf_repokey_validator/jf_repokey_validtaor";
import {jfValidatorDateFormat} from "./validators/jf_validator_date_format";
import {jfValidatorReverseProxyPort} from "./validators/jf_validator_reverse_proxy_port";
import {jfRadioButton} from "./jf_radio_button/jf_radio_button";
import {jfListSelection} from './jf_list_selection/jf_list_selection';
import {jfManageProLicense} from './jf_manage_artifactory_licenses/jf_manage_pro_license';
import {jfManageHaLicenses} from './jf_manage_artifactory_licenses/jf_manage_ha_licenses';
import {jfDragAndDropTxt} from './jf_drag_and_drop_txt/jf_drag_and_drop_txt';
import {jfNews} from './jf_news/jf_news.controller';

//import {jfSidebar}       from './jf_sidebar/jf_sidebar';

// custom field validators
//import {jfValidation}   from './jf_validation/jf_validation';

// UI  Components


angular.module('artifactory.directives',
        ['artifactory.services', 'artifactory.dao', 'ui.select', 'ngSanitize', 'ui.highlight'])
        .directive({
            'jfAccordion': jfAccordion,
            'jfFooter': jfFooter,
            'jfHeader': jfHeader,
            'jfMessages': jfMessages,
            'jfHeaderSearch': jfHeaderSearch,
            //        'jfSidebar': jfSidebar,
            'jfBrowseFiles': jfBrowseFiles,
            'dynamicDirective': dynamicDirective,
            'jfSingleDeploy': jfSingleDeploy,
            'jfMultiDeploy': jfMultiDeploy,
            'jfPrint': jfPrint,
            'jfAutoFocus': jfAutoFocus,
            'jfBodyClass': jfBodyClass,
            'jfInputTextV2': jfInputTextV2,
            'jfSwitchToggle': jfSwitchToggle,
            'jfCronFormatter': jfCronFormatter,
            'jfSpecialChars': jfSpecialChars,
            'jfSpinner': jfSpinner,
            'jfBreadcrumb': jfBreadcrumb,
            'jfRepokeyValidator': jfRepokeyValidator,
            'jfDisableFeature': jfDisableFeature,
            'jfHideForAol': jfHideForAol,
            'jfFileDrop': jfFileDrop,
            'jfAutoComplete': jfAutoComplete,
            'jfDockerV2Layer': jfDockerV2Layer,
            'jfDockerV2Layers': jfDockerV2Layers,
            'rtfactStorageViewer': rtfactStorageViewer,
            'rtfactStorageElement': rtfactStorageElement,
            'rtfactStorageUsage': rtfactStorageUsage,


            'jfValidatorName': jfValidatorName,
            'jfValidatorUniqueId': jfValidatorUniqueId,
            'jfValidatorXmlName': jfValidatorXmlName,
            'jfValidatorCron': jfValidatorCron,
            'jfValidatorLdapUrl': jfValidatorLdapUrl,
            'jfValidatorPathPattern': jfValidatorPathPattern,
            'jfValidatorIntValue': jfValidatorIntValue,
            'jfValidatorDateFormat': jfValidatorDateFormat,
            'jfValidatorMaxTextLength': jfValidatorMaxTextLength,
            'jfValidatorReverseProxyPort': jfValidatorReverseProxyPort,
            'jfValidatorNamePrefixNotNumeric': jfValidatorNamePrefixNotNumeric,

            'jfRadioButton': jfRadioButton,
            'jfListSelection': jfListSelection,
            'jfManageProLicense': jfManageProLicense,
            'jfManageHaLicenses': jfManageHaLicenses,
            'jfDragAndDropTxt': jfDragAndDropTxt,
            'jfNews': jfNews
        })

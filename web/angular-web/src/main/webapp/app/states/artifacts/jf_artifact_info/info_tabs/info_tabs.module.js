import {jfBuilds} from "./jf_builds";
import {jfEffectivePermissions} from "./jf_effective_permissions";
import {jfWatchers} from "./jf_watchers";
import {jfGeneral} from "./jf_general";
import {jfProperties} from "./jf_properties";
import {jfViewSource} from "./jf_view_source";
import {jfPomView} from "./jf_pom_view";
import {jfXmlView} from "./jf_xml_view";
import {jfIvyView} from "./jf_ivy_view";
import {jfNuget} from "./jf_nuget";
import {jfComposer} from "./jf_composer";
import {jfPyPi} from "./jf_pypi";
import {jfHelm} from "./jf_helm";
import {jfPuppet} from "./jf_puppet";
import {jfBower} from "./jf_bower";
import {jfDocker} from "./jf_docker";
import {jfDockerAncestry} from "./jf_docker_ancestry";
import {jfDockerV2} from "./jf_docker_v2";
import {jfRubyGems} from "./jf_ruby_gems";
import {jfNpmInfo} from "./jf_npm_info";
import {jfRpm} from "./jf_rpm_info";
import {jfCocoapods} from "./jf_cocoapods";
import {jfConan} from './jf_conan';
import {jfConanPackage} from './jf_conan_package';
import {jfStashInfo} from "./jf_stash_info";
import {jfDebianInfo} from "./jf_debian_info";
import {jfOpkgInfo} from "./jf_opkg_info";
import {jfChefInfo} from "./jf_chef_info";

export default angular.module('infoTabs', [])
        .directive({
            'jfBuilds': jfBuilds,
            'jfEffectivePermissions': jfEffectivePermissions,
            'jfWatchers': jfWatchers,
            'jfGeneral': jfGeneral,
            'jfProperties': jfProperties,
            'jfViewSource': jfViewSource,
            'jfPomView': jfPomView,
            'jfXmlView': jfXmlView,
            'jfIvyView': jfIvyView,
            'jfNuget': jfNuget,
            'jfComposer': jfComposer,
            'jfPyPi': jfPyPi,
            'jfHelm': jfHelm,
            'jfPuppet': jfPuppet,
            'jfBower': jfBower,
            'jfConan': jfConan,
            'jfConanPackage': jfConanPackage,
            'jfDocker': jfDocker,
            'jfDockerAncestry': jfDockerAncestry,
            'jfDockerV2': jfDockerV2,
            'jfRubyGems': jfRubyGems,
            'jfNpmInfo': jfNpmInfo,
            'jfRpm': jfRpm,
            'jfCocoapods': jfCocoapods,
            'jfStashInfo': jfStashInfo,
            'jfDebianInfo': jfDebianInfo,
            'jfChefInfo': jfChefInfo,
            'jfOpkgInfo': jfOpkgInfo
        });
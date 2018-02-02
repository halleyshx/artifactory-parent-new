import {ReplaceCharacter} from './replace_character';
import {ParseLinks} from './parse_links';
import {ReplaceStringForAol} from './replace_string_for_aol';

export default angular.module('artifactory.filters', [])
        .filter('replaceCharacter', ReplaceCharacter)
        .filter('parseLinks', ParseLinks)
        .filter('replaceStringForAol', ReplaceStringForAol);
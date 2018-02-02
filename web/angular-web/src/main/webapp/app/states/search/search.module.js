import {SearchStateController} from './search.controller';
import {searchQueryMaker}      from './search_query_maker';
import {packageSearch}      from './package_search';


function searchConfig($stateProvider) {
    $stateProvider

        .state('search', {
            url: '/search/{searchType}/{query}',
            parent: 'app-layout',
            templateUrl: 'states/search/search.html',
            controller: 'SearchStateController as SearchController',
            params: {oauthError: null, fromHome: false},
        })
}

export default angular.module('search', [])
    .config(searchConfig)
    .directive('searchQueryMaker', searchQueryMaker)
    .directive('packageSearch', packageSearch)
    .controller('SearchStateController', SearchStateController);


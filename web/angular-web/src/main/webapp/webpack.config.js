var path = require('path');
var webpack = require('webpack');
var CommonsChunkPlugin = require('./node_modules/webpack/lib/optimize/CommonsChunkPlugin');
var CONFIG = require('./artifactory.config');

const rootPath = path.join(__dirname, 'app');

module.exports = {

    context: rootPath,

    entry: {
        artifactory_main: path.join(rootPath, 'app.js'),
        artifactory_services: path.join(rootPath, 'services/artifactory.services.module.js'),
        artifactory_directives: path.join(rootPath, 'directives/artifactory.directives.module.js'),
        artifactory_dao: path.join(rootPath, 'data/artifactory_dao_module'),
        artifactory_ui_components: path.join(rootPath, 'ui_components/ui_components.module'),
        artifactory_states: path.join(rootPath, 'states/artifactory.states.module'),
        artifactory_filters: path.join(rootPath, 'filters/artifactory.filters.module')
    },

    output: {
        path: CONFIG.DESTINATIONS.TARGET,
        filename: '[name].js'
    },

    plugins: [

        new CommonsChunkPlugin({
            name: "artifactory_core",
            filename: "artifactory_core.js",
            chunks: ["artifactory_services", "artifactory_dao"]
        }),

        new CommonsChunkPlugin({
            name: "artifactory_ui",
            filename: "artifactory_ui.js",
            chunks: ["artifactory_directives", "artifactory_ui_components", "artifactory_filters"]
        }),

        new CommonsChunkPlugin({
            name: "artifactory_views",
            filename: "artifactory_views.js",
            chunks: ["artifactory_states"]
        })

    ],

    module: {
        loaders: [
            {test: /\.js$/, loader: 'babel'},
            {test: /\.html$/, loader: 'raw'},

            ]
    },

    devtool: "source-map"
};

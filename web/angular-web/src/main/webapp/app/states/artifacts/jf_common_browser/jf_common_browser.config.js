import types from '../constants/artifact_browser_icons.constant';
module.exports = {

    "core": {
        "animation": false,
        "multiple": false,
        "strings" : { 'Loading ...' : 'Please wait ...' }
    },

    "types": types,
    "search": {
        "depthFirst": true,
        "close_opened_onclear": false
    },
    
    "contextmenu": {
        "select_node": false,
        "show_at_node": false
    },

    "plugins": ["search", "types", "contextmenu", "sort"]
};

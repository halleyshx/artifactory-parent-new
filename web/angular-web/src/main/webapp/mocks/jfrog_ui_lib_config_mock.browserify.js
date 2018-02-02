import EVENTS       from '../app/constants/artifacts_events.constants';

angular.module('conf.fixer', ['jfrog.ui.essentials']).config((JFrogUILibConfigProvider) => {
    JFrogUILibConfigProvider.setConfig({
    customEventsDefinition: EVENTS
});
})
;

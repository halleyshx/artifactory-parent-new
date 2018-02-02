import EVENTS from "../../../../constants/artifacts_events.constants";

export class AdminAdvancedConfigDescriptorController {

    constructor($scope,$timeout, ArtifactoryHttpClient, JFrogNotifications, RESOURCE, ArtifactoryModelSaver, JFrogEventBus) {
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.RESOURCE = RESOURCE;
        this.artifactoryNotifications = JFrogNotifications;
        this.artifactoryHttpClient = ArtifactoryHttpClient;
        this.configDescriptor = '';
        this.apiAccess = {};
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['configDescriptor']);
        this.JFrogEventBus = JFrogEventBus;

        this._getData();

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.REFRESH_PAGE_CONTENT,()=>{
            this._getData();
        });
    }

    _getData() {
        this.artifactoryHttpClient.get(this.RESOURCE.CONFIG_DESCRIPTOR).then((response) => {
                this.configDescriptor = response.data;
        this.ArtifactoryModelSaver.save();
                this.$timeout(()=> {
                    this.apiAccess.api.clearHistory();
                });
            }
        );
    }

    save(configXml) {
        this.artifactoryHttpClient.put(this.RESOURCE.CONFIG_DESCRIPTOR, {configXml})
            .success(response => {
            this.ArtifactoryModelSaver.save();
                this.artifactoryNotifications.create(response)
            })
            .error(response => {
                if (response.errors && response.errors.length) {
                    this.artifactoryNotifications.create(angular.fromJson(response.errors[0].message));
                }
            });
    }

    cancel() {
        this.ArtifactoryModelSaver.ask(true).then(() => {
            this._getData();
        });
    }

}

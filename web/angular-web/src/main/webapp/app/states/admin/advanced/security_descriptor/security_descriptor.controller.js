export class AdminAdvancedSecurityDescriptorController {
    constructor($timeout, ArtifactoryHttpClient, JFrogNotifications, RESOURCE, ArtifactoryModelSaver) {
        this.artifactoryHttpClient = ArtifactoryHttpClient;
        this.artifactoryNotifications = JFrogNotifications;
        this.RESOURCE = RESOURCE;
        this.$timeout = $timeout;
        this.securityDescriptor = '';
        this.apiAccess = {};
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['securityDescriptor']);

        this._getData();
    }

    _getData() {
        this.artifactoryHttpClient.get(this.RESOURCE.SECURITY_DESCRIPTOR).then((response) => {
            this.securityDescriptor = response.data;
        this.ArtifactoryModelSaver.save();
            this.$timeout(()=> {
                this.apiAccess.api.clearHistory();
            });
        });
    }

    save(securityXML) {
        this.artifactoryHttpClient.put(this.RESOURCE.SECURITY_DESCRIPTOR, {securityXML}).
            success((response) => {
            this.ArtifactoryModelSaver.save();
                    this.artifactoryNotifications.create(response);
                }
        )
            .error((response) => {
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
export class LastDeployedController {
    constructor(HomePageDao, $timeout, ArtifactoryDeployModal, GoogleAnalytics) {

        this.homePageDao = HomePageDao;
        this.GoogleAnalytics = GoogleAnalytics;
        this.deployModal = ArtifactoryDeployModal;
        this.data = {};
        this.getData();
    }

    getData() {
        this.lastDeployed;

        this.homePageDao.get({widgetName: 'latestBuilds'}).$promise.then((data)=> {
            this.data = data.widgetData;
            this.lastDeployed = true;
            if (this.lastDeployed) {
                this.$widgetObject.showSpinner = false;
            }
        });
        this.dateTime = (new Date).getTime();
    }

    refresh() {
        this.$widgetObject.showSpinner = true;
        this.getData();
    }

    itemClick() {
        this.GoogleAnalytics.trackEvent('Homepage', 'Last deployed item click');
    }
}
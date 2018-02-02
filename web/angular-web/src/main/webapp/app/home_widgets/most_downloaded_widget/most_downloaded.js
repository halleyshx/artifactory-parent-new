export class MostDownloadedController {
    constructor(HomePageDao, $timeout, ArtifactoryDeployModal, GoogleAnalytics) {

        this.homePageDao = HomePageDao;
        this.GoogleAnalytics = GoogleAnalytics;
        this.deployModal = ArtifactoryDeployModal;
        this.data = {};
        this.getData(false);
    }
    getData(force) {
        this.mostDownloaded;
        this.homePageDao.get({widgetName:'mostDownloaded', force: force}).$promise.then((data)=> {
            data.widgetData.mostDownloaded.forEach((item)=>{
                item.name = item.path.substr(item.path.lastIndexOf('/')+1);
            });
            this.data = data.widgetData;
            this.mostDownloaded = true;
            if (this.mostDownloaded) {
                this.$widgetObject.showSpinner = false;
            }
        });

        this.dateTime = (new Date).getTime();
    }
    refresh() {
        this.$widgetObject.showSpinner = true;
        this.getData(true);
    }
    itemClick() {
        this.GoogleAnalytics.trackEvent('Homepage', 'Most downloaded item click');
    }
}
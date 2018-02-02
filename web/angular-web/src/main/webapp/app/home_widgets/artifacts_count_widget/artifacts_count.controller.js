export class ArtifactCountController {
    constructor(HomePageDao) {
        this.homePageDao = HomePageDao;
        this.initHomePage();
        this.updateCount();
    }

    initHomePage() {
        this.homePageDao.get({widgetName:'artifactCount'}).$promise.then((data)=> {
            this.homepageData = data.widgetData;
            this.$widgetObject.showSpinner = false;
        });
    }

    updateCount() {
        this.$widgetObject.showSpinner = true;
        this.initHomePage();

    }

}
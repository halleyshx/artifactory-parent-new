export class VersionController {
    constructor(HomePageDao) {
        this.homePageDao = HomePageDao;
        this.initHomePage();
    }
    initHomePage() {
        this.homePageDao.get({widgetName:'info'}).$promise.then((data)=> {
            this.homepageData = data.widgetData;
            this.$widgetObject.showSpinner = false;
        });
    }
}
export class BaseController {
    constructor(FooterDao,ArtifactorySidebarDriver,$timeout) {

        this.FooterDao = FooterDao;
        this.$timeout = $timeout;

        this.getFooterData();

        this.sidebarDriver = ArtifactorySidebarDriver;
    }

    getFooterData(force = false) {
        // Ensure page is not displayed before we get the footer data
        this.FooterDao.get(force).then(footerData => this.footerData = footerData);


        // Check that we have the footer data, solve RTFACT-13069 (Happens inconsistently when restarting server / starting vanilla)
        this.$timeout(()=>{
            if (!this.footerData) {
                this.getFooterData(true);
            }
        },100)
    }
}

export class AdminConfigurationRegisterController{

    constructor(User, $state,FooterDao) {
        this.$state = $state;
        this.User=User;
        this.footerDao = FooterDao;
        this.initHa();
    }

    initHa(){
        this.footerDao.get().then((footerData)=>{
            this.isHaConfigured = footerData.haConfigured;
        });
    }

}
import EVENTS from '../../constants/artifacts_events.constants';
import TOOLTIP from '../../constants/artifact_tooltip.constant';

class jfManageProLicenseController {

    constructor(RegisterProDao, JFrogEventBus, User, $state, ArtifactoryModelSaver, ArtifactoryState, JFrogNotifications) {
        this.ArtifactoryState = ArtifactoryState;
        this.registerProDao = RegisterProDao;
        this.$state = $state;
        this.JFrogEventBus = JFrogEventBus;
        this.User=User;
        this.artifactoryNotifications = JFrogNotifications;
        this.TOOLTIP = TOOLTIP.admin.configuration.registerPro;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['registerDetails']);
        this.getData();
        this.dndHeadingHtml = `Enter your license key below or`+
                              `<span class="drop-file-label"> drop a file 
                                <i class="icon icon-upload"></i>
                               </span>.`;
        this.dndStyle = {'width':"100%",'height':"230px"};
        this.dndOnError = (errorMessage) =>{
            this.artifactoryNotifications.create({
                error: errorMessage
            });
        };
    }

    removeComments(text){
        return text.replace(/#+((?:.)+?)*/g,'');
    }

    save(registerDetails) {
        // Remove unnecessary comments
        registerDetails.key = this.removeComments(registerDetails.key);
        // Save
        this.registerProDao.update(registerDetails).$promise.then( (data)=> {
            // Refresh the home page footer with the new license details
            this.JFrogEventBus.dispatch(EVENTS.FOOTER_REFRESH);

            // Initialize the 'has licanse already' state
            let initStatus = this.ArtifactoryState.getState('initStatus');
            if (initStatus) initStatus.hasLicenseAlready = true;

            this.User.loadUser(true).then(()=>this.getData());

            // Upon successful installation display the new license
            // if (data.status === 200) this.registerDetails.key = data.key;
        });
    }

    getData() {
        if(this.User.currentUser.isProWithoutLicense()){
            this.registerDetails = "";
            this.ArtifactoryModelSaver.save();
        }
        else {
            this.registerProDao.get(true).$promise.then((data)=>{
                this.registerDetails = data;
                this.ArtifactoryModelSaver.save();
            });
        }
    }

    reset() {
        this.ArtifactoryModelSaver.ask(true).then(() => {
            this.getData();
        });

    }
}

export function jfManageProLicense() {

    return {
        restrict: 'E',
        scope: {items: '='},
        controller: jfManageProLicenseController,
        controllerAs: 'jfManageProLicense',
        templateUrl: 'directives/jf_manage_artifactory_licenses/jf_manage_pro_license.html',
        bindToController: true
    };
}

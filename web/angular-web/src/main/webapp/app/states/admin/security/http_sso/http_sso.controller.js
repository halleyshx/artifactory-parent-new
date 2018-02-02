import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityHttpSSoController {

    constructor(HttpSsoDao, JFrogEventBus, ArtifactoryModelSaver) {
        this.JFrogEventBus = JFrogEventBus;
        this.httpSsoDao = HttpSsoDao.getInstance();
        this.sso = this.getSsoData();
        this.TOOLTIP = TOOLTIP.admin.security.HTTPSSO;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['sso']);
        this.EVENTS = JFrogEventBus.getEventsDefinition();

    }

    getSsoData() {
        this.httpSsoDao.get().$promise.then((sso)=> {
            this.sso = sso;
        this.ArtifactoryModelSaver.save();
        this.JFrogEventBus.dispatch(this.EVENTS.FORM_CLEAR_FIELD_VALIDATION, true);
        });
    }

    reset() {
        this.ArtifactoryModelSaver.ask(true).then(() => {
            this.getSsoData();
        });
    }
    save(sso) {
        this.httpSsoDao.update(sso).$promise.then(()=>{
            this.ArtifactoryModelSaver.save();
        });
    }
}
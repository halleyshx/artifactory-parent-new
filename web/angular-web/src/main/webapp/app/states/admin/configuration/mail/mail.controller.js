import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationMailController {

    constructor(MailDao, JFrogEventBus, $timeout, ArtifactoryModelSaver) {
        this.mailDao = MailDao.getInstance();
        this.JFrogEventBus = JFrogEventBus;
        this.getMailData();
        this.mailSettingsForm = null;
        this.testReceiptForm = null;
        this.TOOLTIP = TOOLTIP.admin.configuration.mail;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['mail']);
        this.$timeout = $timeout;
        this.EVENTS = JFrogEventBus.getEventsDefinition();
    }

    getMailData() {
        this.mailDao.get().$promise.then((mail)=> {
            this.mail = mail;
        this.ArtifactoryModelSaver.save();
        this.JFrogEventBus.dispatch(this.EVENTS.FORM_CLEAR_FIELD_VALIDATION, true);
        });
    }

    save(form) {
        this.JFrogEventBus.dispatch(this.EVENTS.FORM_SUBMITTED, form.$name);
        if (this.mailSettingsForm.$valid) {
            this.mailDao.update(this.mail).$promise.then(()=>{
                this.ArtifactoryModelSaver.save();
            });
        }
    }

    reset() {
        this.ArtifactoryModelSaver.ask(true).then(() => {
            this.getMailData();
        });

    }
    testReceipt(form) {
        this.JFrogEventBus.dispatch(this.EVENTS.FORM_SUBMITTED, form.$name);
        if (this.testReceiptForm.$valid) {
            this.mailDao.save(this.mail);
        }
    }
}
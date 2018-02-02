const EMAIL_SENT_MESSAGE = "Reset password email was sent. \nDidn't received it? Contact your system administrator.";

export class ForgotPasswordController {

    constructor($state, User, JFrogNotifications, JFrogEventBus) {
        this.user = {};
        this.UserService = User;
        this.$state = $state;
        this.artifactoryNotifications = JFrogNotifications;
        this.JFrogEventBus = JFrogEventBus;
        this.forgotPasswordForm = null;
        this.message = '';
        this.EVENTS = JFrogEventBus.getEventsDefinition();
    }

    forgot() {
        let self = this;

        this.JFrogEventBus.dispatch(this.EVENTS.FORM_SUBMITTED);
        if (this.forgotPasswordForm.$valid) {
            this.pending = true;
            this.UserService.forgotPassword(this.user).then(success, error)
        } else {
            form.user.$dirty = true;
        }

        function success(result) {
            self.pending = false;
            self.$state.go('login');
            self.artifactoryNotifications.create({info: EMAIL_SENT_MESSAGE});
        }

        function error(errors) {
            self.pending = false;
            self.$state.go('login');
        }
    }
}
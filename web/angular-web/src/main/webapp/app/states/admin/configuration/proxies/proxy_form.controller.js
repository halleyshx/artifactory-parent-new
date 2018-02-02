import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

let $state, $stateParams, ProxiesDao, JFrogModal, ArtifactoryModelSaver;

export class AdminConfigurationProxyFormController {

    constructor(_$state_, _$stateParams_, _ProxiesDao_, _JFrogModal_, _ArtifactoryModelSaver_) {
        ProxiesDao = _ProxiesDao_;
        $stateParams = _$stateParams_;
        $state = _$state_;
        JFrogModal = _JFrogModal_;
        ArtifactoryModelSaver = _ArtifactoryModelSaver_.createInstance(this,['proxy']);;


        this.isNew = !$stateParams.proxyKey;
        this.formTitle = `${this.isNew && 'New' || 'Edit ' + $stateParams.proxyKey } Proxy`;
        this.TOOLTIP = TOOLTIP.admin.configuration.proxyForm;
        this._initProxy();
    }

    _initProxy() {
        if (this.isNew) {
            this.proxy = {};
        }
        else {
            ProxiesDao.getSingleProxy({key: $stateParams.proxyKey}).$promise
                .then((proxy) => {
                        this.proxy = proxy;
                        this.proxy.redirectedToHostsArray = this.proxy.redirectedToHosts ? this.proxy.redirectedToHosts.split(',') : [];
                        ArtifactoryModelSaver.save();
                    });
        }
    }

    onChangeDefault() {
        if (!this.proxy.defaultProxy) return;
        JFrogModal.confirm('Do you wish to use this proxy with existing remote repositories (and override any assigned proxies)?',
                '',
                {confirm: "OK"})
            .catch(() => this.proxy.defaultProxy = false);
    }

    save() {
        if (this.savePending) return;

        this.savePending = true;

        this.proxy.redirectedToHosts = this.proxy.redirectedToHostsArray ? this.proxy.redirectedToHostsArray.join(',') : undefined;

        let whenSaved = this.isNew ? ProxiesDao.save(this.proxy) : ProxiesDao.update(this.proxy);
        whenSaved.$promise.then(() => {
            ArtifactoryModelSaver.save();
            this._end()
            this.savePending = false;
        }).catch(()=>this.savePending = false);
    }

    cancel() {
        this._end();
    }

    _end() {
        $state.go('^.proxies');
    }
}
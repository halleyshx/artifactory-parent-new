import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

let $stateParams, LdapDao, $state, JFrogGridFactory;

export class LdapSettingFormController {
    constructor(_$stateParams_, _$state_, _LdapDao_, _JFrogGridFactory_, JFrogNotifications, ArtifactoryModelSaver, ArtifactoryFeatures) {
        $state = _$state_;
        $stateParams = _$stateParams_;
        LdapDao = _LdapDao_;
        JFrogGridFactory = _JFrogGridFactory_;
        this.artifactoryNotifications = JFrogNotifications;
        this.testConnection = {};
        this.isNew = !$stateParams.ldapSettingKey;
        this.TOOLTIP = TOOLTIP.admin.security.LDAPSettingsForm;
        this.features = ArtifactoryFeatures;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['ldap']);
        this._initLdapSetting();
    }

    _initLdapSetting() {
        if (this.isNew) {
            this.ldap = {
                enabled: true,
                autoCreateUser: true,
                search: {searchSubTree: true},
                emailAttribute: 'mail',
                ldapPoisoningProtection: true
            };
        }
        else {
            LdapDao.get({key: $stateParams.ldapSettingKey}).$promise
                    .then((ldapSetting) => {
                        this.ldap = ldapSetting
            this.ArtifactoryModelSaver.save();
                    });
        }
    }

    save() {
        if (this.savePending) return;

        this.savePending = true;

        if (this.ldapEditForm.$valid) {
            if (!this.ldap.userDnPattern && !this.ldap.search.searchFilter) {
                this.messageUserOrSearch();
            }
            else {
                if (this.ldap.search && this._isSearchFieldsNull(this.ldap.search)) {
                    this.ldap.search = undefined;
                }
                let whenSaved = this.isNew ? LdapDao.save(this.ldap) : LdapDao.update(this.ldap);
                whenSaved.$promise.then(() => {
                    this.savePending = false;
                    this.ArtifactoryModelSaver.save();
                    this._end()
                }).catch(()=>this.savePending = false);
            }
        }
    }

    _isSearchFieldsNull(search) {
        return (!search.managerDn && !search.managerPassword && !search.searchBase && !search.searchFilter);
    }

    cancel() {
        this._end();
    }

    _end() {
        $state.go('^.ldap_settings');
    }

    doTestConnection() {
        if (this.ldapEditForm.$valid) {
            if (!this.ldap.userDnPattern && !this.ldap.search.searchFilter) {
                this.messageUserOrSearch();
            }
            else {
                var testData = {};
                _.extend(testData, this.ldap);
                _.extend(testData, this.testConnection);

                LdapDao.test(testData);
            }
        }
    }

    messageUserOrSearch() {
        this.artifactoryNotifications.create({error: 'LDAP settings should provide a userDnPattern or a searchFilter (or both)'});
        this.savePending = undefined;
    }

    testConnectionOnEnterPress($event) {
        $event.preventDefault();
        if (!!this.testConnection.testUsername && !!this.testConnection.testPassword) {
            this.doTestConnection();
        }
    }
}
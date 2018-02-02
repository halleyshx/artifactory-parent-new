import EVENTS from '../../constants/artifacts_events.constants';
const VERSION_INFO_KEY = 'VERSION_INFO';
export class FooterDao {
    constructor(RESOURCE, ArtifactoryDaoFactory, ArtifactoryStorage, $timeout, JFrogEventBus) {
		this.storage = ArtifactoryStorage;
        this.$timeout = $timeout;
        this.JFrogEventBus = JFrogEventBus;
    	this._resource = ArtifactoryDaoFactory()
            .setPath(RESOURCE.FOOTER)
            .getInstance();
        this.retries = 0;
    }

    get(force = false) {

        if (this.retries >= 10) return this.cached;

        if (!this.cached || force) {
            this.cached = this._resource.get().$promise
                    .then(info => this._info = info);
        }

        //Fix for RTFACT-9873
        if (!this._info) {
            this.$timeout(()=> {
                if (!this._info) {
                    this.retries++;
                    this.get(true).then(()=> {
                        this.JFrogEventBus.dispatch(EVENTS.FOOTER_DATA_UPDATED);
                    });
                }
                else if (this._info) {
                    this.retries = 0;
                }
            }, 400);
        }
        else if (this._info) {
            this.retries = 0;
        }

        return this.cached;
    }

    getInfo() {
        return this._info;
    }
}

import fieldOptions from "../../constants/field_options.constats";

export class SetMeUpWidgetController {
    constructor(TreeBrowserDao, SetMeUpModal, JFrogEventBus, ArtifactoryState, GoogleAnalytics) {

        this.treeBrowserDao = TreeBrowserDao;
        this.ArtifactoryState = ArtifactoryState;
        this.GoogleAnalytics = GoogleAnalytics;
        this.SetMeUpModal = SetMeUpModal;

        let EVENTS = JFrogEventBus.getEventsDefinition();

        JFrogEventBus.register(EVENTS.REFRESH_SETMEUP_WIZARD,()=>{
            this.$widgetObject.showSpinner = true;
            this.getRepos();
        });

        this.packageTypes = _.cloneDeep(fieldOptions.repoPackageTypes);
        this.getRepos();
    }

    getRepos() {
        this.treeBrowserDao.getRoots(true).then((repos)=>{
            let repoOrder = this.ArtifactoryState.getState('repoOrder');
            let repoScore = this._getScoreObjectFromOrderArray(repoOrder || ['DISTRIBUTION', 'LOCAL', 'REMOTE', 'VIRTUAL']);

            this.repos = _.map(_.filter(repos,repo=>repo.repoType !== 'trash' && repo.repoType !== 'distribution'),repo=>{
                let packageType = _.find(this.packageTypes,{serverEnumName: repo.repoPkgType});
                if (packageType) repo.icon = packageType.icon;
                return repo;
            });

            this.repos = this.filterCacheDoubles(this.repos);

            this.repos = _.sortBy(this.repos, repo=>-repoScore[repo.repoType]);

            this.$widgetObject.showSpinner = false;
        })
    }

    filterCacheDoubles(repos) {
        let DASH_CACHE = '-cache';
        let cacheRepos = _.filter(repos,repo=>_.contains(repo.repoKey,DASH_CACHE));

        cacheRepos.forEach(repo=>{
            let remoteRepoKey = repo.repoKey.substr(0,repo.repoKey.length - DASH_CACHE.length);
            let remote = _.find(repos,{repoKey: remoteRepoKey});
            if (remote) { //We have a double, just remove the cached one from array
                repos.splice(repos.indexOf(repo),1);
            }
            else { //We have only cache repo, change it's name to not include '-cache'
                repo.repoKey = remoteRepoKey;
            }
        });

        return repos;
    }

    showSetMeUp(repo) {
        this.GoogleAnalytics.trackEvent('Homepage' , 'Quick set me up' , repo.repoPkgType, null, repo.repoType);
        this.SetMeUpModal.launch(repo, true);
    }

    _getScoreObjectFromOrderArray(order) {
        let repoScore = {};
        let score = 100000;
        order.forEach((repoType) => {
            repoScore[repoType.toLowerCase()] = score;
            if (repoType === 'REMOTE') {
                repoScore['cached'] = score;
                score = score / 10;
            }
            score = score / 10;
        });
        return repoScore;
    }

    filterHasNoMatches() {
        if (!this.repoFilter) return false;

        let count = _.filter(this.repos, (repo)=>_.contains(repo.repoKey,this.repoFilter)).length;
        return count === 0;
    }

}
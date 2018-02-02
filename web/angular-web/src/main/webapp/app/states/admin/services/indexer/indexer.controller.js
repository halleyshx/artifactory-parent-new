import TOOLTIP from '../../../../constants/artifact_tooltip.constant';
import EVENTS from "../../../../constants/artifacts_events.constants";

export class AdminServicesIndexerController {
    constructor($scope,IndexerDao, RepositoriesDao, ArtifactoryModelSaver,JFrogEventBus) {
        this.$scope = $scope;
        this.indexerDao = IndexerDao.getInstance();
        this.repositoriesDao = RepositoriesDao;
        this.indexer = {};
        this.TOOLTIP = TOOLTIP.admin.services.mavenIndexer;
        this.ArtifactoryModelSaver = ArtifactoryModelSaver.createInstance(this, ['indexer']);
        this.JFrogEventBus = JFrogEventBus;
        this.getIndexerObject();

        this.JFrogEventBus.registerOnScope(this.$scope, EVENTS.REFRESH_PAGE_CONTENT,()=>{
            this.repositoriesDao.indexerAvailableRepositories({type: 'Maven', layout: 'maven-2-default'}).$promise.then((repoData) => {
                let repoList = repoData.availableLocalRepos.concat(repoData.availableRemoteRepos).concat(repoData.availableVirtualRepos);
                this.addRepos(repoList);
            });
        });
    }

    getIndexerObject() {
        this.indexerDao.get().$promise.then((result) => {
            this.indexer = result;
            this.ArtifactoryModelSaver.save();
            this.getRepoData();
        });
    }

    addRepos(repoList){
        repoList.forEach((repo)=> {
            if (this.indexer.excludedRepos.indexOf(repo) == -1
                    && this.indexer.includedRepos.indexOf(repo) == -1) {
                this.indexer.includedRepos.push(repo);
            }
        });

        this.ArtifactoryModelSaver.save();
    }

    getRepoData() {
        if (!this.indexer.includedRepos) {
            this.repositoriesDao.indexerAvailableRepositories({type: 'Maven', layout: 'maven-2-default'}).$promise.then((repos) => {
                this.indexer.includedRepos = [];
                this.indexer.excludedRepos = [];
                this.indexer.includedRepos = repos.availableLocalRepos.concat(repos.availableRemoteRepos).concat(repos.availableVirtualRepos);
            });
        }
    }

    runIndexer() {
        this.indexerDao.run(this.indexer);
    }

    save(indexer) {
        this.indexerDao.save(indexer).$promise.then(()=>{
            this.ArtifactoryModelSaver.save();
        });
    }

    cancel() {
        this.ArtifactoryModelSaver.ask(true).then(() => {
            this.getIndexerObject();
        });
    }
}
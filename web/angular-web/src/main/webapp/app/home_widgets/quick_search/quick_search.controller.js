export class QuickSearchController {
    constructor($state, ArtifactoryFeatures, GoogleAnalytics) {
        this.GoogleAnalytics = GoogleAnalytics;
        this.$state = $state;
        this.features = ArtifactoryFeatures;

        this.links = [
            {
                title: 'Package\nSearch',
                search: 'package'
            },
            {
                title: 'Archive\nSearch',
                search: 'archive'
            },
            {
                title: 'Property\nSearch',
                search: 'property'
            },
            {
                title: 'Checksum\nSearch',
                search: 'checksum'
            },
            {
                title: 'JCenter\nSearch',
                search: 'remote'
            },
        ]
    }

    search() {
        if (!this.query) return;

        let query = {
            "search": "quick",
            "query": this.query
        }
        this.$state.go('search',{searchType: 'quick', query: btoa(JSON.stringify(query)), fromHome: true});
    }

    gotoSearch(searchType) {
        this.GoogleAnalytics.trackEvent('Homepage' , 'Quick Search link' , searchType);
        this.$state.go('search',{searchType: searchType});
    }

}
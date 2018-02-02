export class UpdatesController {
    constructor(User,$timeout) {
        this.$timeout = $timeout;
        this.showNews = false;
        let offlineMode = User.getCurrent().offlineMode;
        if (!offlineMode) this.readUpdateHTML();
    }

    readUpdateHTML() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'https://service.jfrog.org/artifactory/updatesv4', true);
        xhr.onreadystatechange= ()=>{
            this.updateHTML=xhr.response;
            this.$scope.$apply();

            //twitter button javascript !
            !function(d,s,id){
                var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';
                if(!d.getElementById(id)){
                    js=d.createElement(s);
                    js.id=id;js.src=p+'://platform.twitter.com/widgets.js';
                    fjs.parentNode.insertBefore(js,fjs);
                }
            }(document, 'script', 'twitter-wjs');

            this.$scope.$on('$destroy', () => {
                let twitter = document.getElementById('twitter-wjs');
                if (twitter) twitter.remove();
            });

            if(xhr.response) {
                this.$timeout(()=>{
                    this.showNews = true;
                    this.$widgetObject.showSpinner = false;
                },200);
            }
        };
        xhr.send();
    }

}
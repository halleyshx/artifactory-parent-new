export class LinksController {
    constructor(GoogleAnalytics) {
        this.GoogleAnalytics = GoogleAnalytics;

        this.links = [
            {
                linkText: 'User Guide',
                class: 'user-guide',
                url: 'https://service.jfrog.org/artifactory/home/userguide',
                svg: 'images/userguide.svg'
            },
            {
                linkText: 'Webinar Signup',
                class: 'webinar',
                url: 'https://service.jfrog.org/artifactory/home/webinars',
                svg: 'images/webinar.svg'
            },
            {
                linkText: 'Support Portal',
                class: 'support',
                url: 'https://service.jfrog.org/artifactory/home/supportportal',
                svg: 'images/support.svg'
            },
            {
                linkText: 'Stackoverflow',
                class: 'stackoverflow',
                url: 'https://service.jfrog.org/artifactory/home/stackoverflow',
                svg: 'images/stackoverflow.svg'
            },
            {
                linkText: 'Blog',
                class: 'blogs',
                url: 'https://service.jfrog.org/artifactory/home/blog',
                svg: 'images/blogs.svg'
            },
            {
                linkText: 'Rest API',
                class: 'rest-api',
                url: 'https://service.jfrog.org/artifactory/home/restapi',
                svg: 'images/rest_api.svg'
            }
        ];

    }

    linkClick(linkText) {
        this.GoogleAnalytics.trackEvent('Homepage','Knowledge Resources Link',linkText)
    }

}
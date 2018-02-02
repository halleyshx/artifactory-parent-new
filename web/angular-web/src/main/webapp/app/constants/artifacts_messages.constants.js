/**
 * Created by tomere on 2/22/2017.
 */
export default{
    actions:{
        distribute:{
            noRepos:{
                admin: {
                    message: `No distribution repositories are configured. To distribute artifacts and builds, <a href="#/admin/repositories/distribution">create a Distribution repository</a>.
                              To learn about distribution repositories, refer to the Artifactory <a href="https://www.jfrog.com/confluence/display/RTF/Distribution+Repository">User Guide <i class="icon icon-external-link"></i></a>.`,
                    messageType: 'alert-info'
                },
                nonAdmin: {
                    message: `No distribution repositories are configured.
                              To learn about distribution repositories, refer to the Artifactory <a href="https://www.jfrog.com/confluence/display/RTF/Distribution+Repository">User Guide <i class="icon icon-external-link"></i></a>.`,
                    messageType: 'alert-info'
                }
            },
            inOfflineMode: {
                message: `Global offline mode is enabled. To allow distribution, disable the global offline mode through the General Configuration page.`,
                messageType: 'alert-danger'
            },
            noPermissions: {
                message: `You do not have distribute and deploy permissions.`,
                messageType: 'alert-danger'
            }
        },
        deploy:{
            deployToDistRepoErrorMessage:{
                message:`File(s) cannot be directly deployed to a distribution repository. Instead, use the "Distribute" action on the relevant repository or select an alternative target repository.`,
                messageType:`alert-danger`
            },
            deployPermissionsErrorMessage:{
                message:`You do not have deploy permission`,
                messageType:`alert-danger`
            },
            hasNoDefaultDeployRepo:{
                message:`This virtual repository is not configured with a default deployment repository. To learn about configuring virtual repositories, refer to the <a href="https://www.jfrog.com/confluence/display/RTF/Deploying+Artifacts#DeployingArtifacts-DeployingtoaVirtualRepository" target="_blank">Artifactory User Guide <i class="icon icon-external-link"></i></a>.`,
                messageType:`alert-warning`
            },
            cannotDeployToRemote:{
                message:`Cannot deploy to a remote repository. To learn about remote repositories, refer to the <a href="https://www.jfrog.com/confluence/display/RTF/Remote+Repositories" target="_blank">Artifactory User Guide <i class="icon icon-external-link"></i></a>.`,
                messageType:`alert-danger`
            },
            cannotDeployToTrashCan:{
                message:`Cannot deploy to Trash Can. To learn about the Trash Can, refer to the <a href="https://www.jfrog.com/confluence/display/RTF/Browsing+Artifactory#BrowsingArtifactory-TrashCan" target="_blank">Artifactory User Guide <i class="icon icon-external-link"></i></a>.`,
                messageType:`alert-danger`
            },
        }
    },
    jf_general:{
        xray:{
            xrayDetectedIssuesOnDownloadableArtifact:`Xray has detected issues on this artifact`,
            xrayDidntScanFileYet:`This artifact is currently blocked for download until it has been indexed and scanned by Xray. Please try again once processing is completed.`,
            xrayDetectedIssuesOnNonDownloadableArtifact:`Xray has detected issues on this artifact, and it has been blocked for download and distribution`,
        }
    },
    set_me_up:{
        puppet:{
            puppetClientVersion:`If you are using Puppet version 4.9.1 and below, you need to modify your reverse proxy configuration. For details, refer to <a href="https://www.jfrog.com/confluence/display/RTF/Puppet+Repositories#PuppetRepositories-UsingPuppet4.9.1andBelow" target="_blank">JFrog Artifactory User Guide <i class="icon icon-external-link"></i></a>.`
        },
        hasNoDeployPermissions:{
            message:`You do not have deploy permissions to this repository`
        },
        hasNoRepositoriesOfType:{
            message:`No repositories match the selected tool`
        }
    }
};
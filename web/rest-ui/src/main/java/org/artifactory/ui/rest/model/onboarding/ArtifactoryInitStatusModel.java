package org.artifactory.ui.rest.model.onboarding;

import org.artifactory.rest.common.model.BaseModel;

/**
 * Returns init status of artifactory, to be used in onboarding wizard in home screen
 *
 * @author nadavy
 */
public class ArtifactoryInitStatusModel extends BaseModel {

    private boolean hasRepos;
    private boolean hasLicenseAlready;
    private boolean hasPriorLogins;
    private boolean hasProxies;
    private boolean skipWizard;

    public ArtifactoryInitStatusModel(boolean hasRepos, boolean hasLicenseAlready, boolean hasPriorLogins,
            boolean hasProxies, boolean skipWizard) {
        this.hasRepos = hasRepos;
        this.hasLicenseAlready = hasLicenseAlready;
        this.hasPriorLogins = hasPriorLogins;
        this.hasProxies = hasProxies;
        this.skipWizard = skipWizard;
    }

    public ArtifactoryInitStatusModel() {
    }

    public boolean isHasRepos() {
        return hasRepos;
    }

    public boolean isHasLicenseAlready() {
        return hasLicenseAlready;
    }

    public boolean isHasPriorLogins() {
        return hasPriorLogins;
    }

    public boolean isSkipWizard() {
        return skipWizard;
    }

    public boolean isHasProxies() {
        return hasProxies;
    }
}


package org.artifactory.addon.xray;

/**
 * @author Chen Keinan
 */
public class XrayScanBuild {

    private String buildName;
    private String buildNumber;
    private String artifactoryId;
    private String context;

    public XrayScanBuild(String buildName, String buildNumber,String context) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
         this.context = context;
    }

    public XrayScanBuild() {
        // for jackson
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getArtifactoryId() {
        return artifactoryId;
    }

    public void setArtifactoryId(String artifactoryId) {
        this.artifactoryId = artifactoryId;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }
}

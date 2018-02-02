package org.artifactory.addon.debian;

import java.util.List;

/**
 * @author Yuval Reches
 */
public class DebianMetadataInfo {

    private DebianInfo debianInfo;
    private List<String> debianDependencies;
    private String failReason;

    DebianMetadataInfo() {

    }

    DebianMetadataInfo(DebianInfo debianInfo, List<String> debianDependencies, String failReason) {
        this.debianInfo = debianInfo;
        this.debianDependencies = debianDependencies;
        this.failReason = failReason;
    }

    public DebianInfo getDebianInfo() {
        return debianInfo;
    }

    public void setDebianInfo(DebianInfo debianInfo) {
        this.debianInfo = debianInfo;
    }

    public List<String> getDebianDependencies() {
        return debianDependencies;
    }

    public void setDebianDependencies(List<String> debianDependencies) {
        this.debianDependencies = debianDependencies;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}

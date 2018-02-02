package org.artifactory.storage.db.build.entity;

/**
 * @author Shay Bagants
 */
public class BuildIdEntity {
    private final long buildId;
    private final String buildName;
    private final String buildNumber;
    private final long buildDate;

    public BuildIdEntity(long buildId, String buildName, String buildNumber, long buildDate) {
        this.buildId = buildId;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.buildDate = buildDate;
    }

    public long getBuildId() {
        return buildId;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public long getBuildDate() {
        return buildDate;
    }
}

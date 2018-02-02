package org.artifactory.storage.db.build.entity;

import org.apache.commons.lang.StringUtils;

/**
 * The base record is used in testing with lot's of properties
 */
public class BuildEntityRecord {
    protected final long buildId;
    protected final String buildName;
    protected final String buildNumber;
    protected final long buildDate;
    protected final String ciUrl;
    protected final long created;
    protected final String createdBy;
    protected final long modified;
    protected final String modifiedBy;

    BuildEntityRecord( BuildEntityRecord record) {
        this(  record.buildId,
                record.buildName,
                record.buildNumber,
                record.buildDate,
                record.ciUrl,
                record.created,
                record.createdBy,
                record.modified,
                record.modifiedBy
        );
    }
    public BuildEntityRecord(
            long buildId, String buildName, String buildNumber, long buildDate, String ciUrl, long created,
            String createdBy, long modified, String modifiedBy) {
        this.buildName = buildName;
        this.created = created;
        this.modified = modified;
        this.buildNumber = buildNumber;
        this.ciUrl = ciUrl;
        this.modifiedBy = modifiedBy;
        this.buildDate = buildDate;
        this.buildId = buildId;
        this.createdBy = createdBy;
        if (buildId <= 0L) {
            throw new IllegalArgumentException("Build id cannot be zero or negative!");
        }
        if (StringUtils.isBlank(buildName) || StringUtils.isBlank(buildNumber) || buildDate <= 0L) {
            throw new IllegalArgumentException("Build name, number and date cannot be empty or null!");
        }
        if (created <= 0L) {
            throw new IllegalArgumentException("Created date cannot be zero or negative!");
        }
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

    public String getCiUrl() {
        return ciUrl;
    }

    public long getCreated() {
        return created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getModified() {
        return modified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }
}

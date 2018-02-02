package org.artifactory.storage.db.build.service;

import org.artifactory.build.BuildId;
import org.artifactory.build.BuildInfoUtils;

import java.util.Date;
import java.util.Objects;

/**
 * @author Shay Bagants
 */
public class BuildIdImpl implements BuildId {
    private final long buildId;
    private final String name;
    private final String number;
    private final long started;

    public BuildIdImpl(long buildId, String name, String number, long started) {
        this.buildId = buildId;
        this.name = name;
        this.number = number;
        this.started = started;
    }

    public long getBuildId() {
        return buildId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNumber() {
        return number;
    }

    /**
     * Returns the starting time of the build
     *
     * @return Build start time
     */
    @Override
    public String getStarted() {
        if (started > 0L) {
            return BuildInfoUtils.formatBuildTime(started);
        } else {
            return "";
        }
    }

    /**
     * Returns a date representation of the build starting time
     *
     * @return Build started date
     * @throws java.text.ParseException
     */
    @Override
    public Date getStartedDate() {
        return new Date(BuildInfoUtils.parseBuildTime(getStarted()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildIdImpl)) return false;
        BuildIdImpl buildID = (BuildIdImpl) o;
        return started == buildID.started &&
                Objects.equals(name, buildID.name) &&
                Objects.equals(number, buildID.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, number, started);
    }
}

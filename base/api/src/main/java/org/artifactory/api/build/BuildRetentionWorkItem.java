package org.artifactory.api.build;

import org.artifactory.api.repo.WorkItem;
import org.artifactory.build.BuildRun;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Liza Dashevski
 */
public class BuildRetentionWorkItem implements WorkItem {
    private final BuildRun buildId;
    private final boolean deleteArtifacts;

    public BuildRetentionWorkItem(BuildRun buildId, boolean deleteArtifacts) {
        this.buildId = Objects.requireNonNull(buildId);
        this.deleteArtifacts = deleteArtifacts;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        return buildId.toString();
    }

    public BuildRun getBuildId() {
        return buildId;
    }

    public boolean isDeleteArtifacts() {
        return deleteArtifacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildRetentionWorkItem that = (BuildRetentionWorkItem) o;
        return deleteArtifacts == that.deleteArtifacts &&
                Objects.equals(buildId, that.buildId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildId, deleteArtifacts);
    }
}

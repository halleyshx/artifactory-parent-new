package org.artifactory.storage.jobs.migration.pathchecksum;

import org.artifactory.api.repo.WorkItem;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author gidis
 */
public class RepoPathChecksumCalculationWorkItem implements WorkItem {

    private RepoPath repoPath;
    private RepoPathChecksumMigrationJobDelegate delegate;

    RepoPathChecksumCalculationWorkItem(RepoPath repoPath, RepoPathChecksumMigrationJobDelegate delegate) {
        this.repoPath = repoPath;
        this.delegate = delegate;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public RepoPathChecksumMigrationJobDelegate getDelegate() {
        return delegate;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        //Only one thread is allowed to run on a single sha1 value so we don't do double calculations
        return repoPath.toPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoPathChecksumCalculationWorkItem)) {
            return false;
        }
        RepoPathChecksumCalculationWorkItem workItem = (RepoPathChecksumCalculationWorkItem) o;
        return Objects.equals(repoPath, workItem.getRepoPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRepoPath());
    }
}

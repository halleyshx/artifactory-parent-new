package org.artifactory.storage.jobs.migration.sha256;

import org.artifactory.api.repo.WorkItem;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Dan Feldman
 */
public class ChecksumCalculationWorkItem implements WorkItem {

    private final String sha1;
    private final Collection<RepoPath> paths;
    private final Sha256MigrationJobDelegate delegate;

    public ChecksumCalculationWorkItem(String sha1, Collection<RepoPath> paths, Sha256MigrationJobDelegate delegate) {
        this.sha1 = sha1;
        this.paths = paths;
        this.delegate = delegate;
    }

    public String getSha1() {
        return sha1;
    }

    public Collection<RepoPath> getPaths() {
        return paths;
    }

    public Sha256MigrationJobDelegate getDelegate() {
        return delegate;
    }

    @Nonnull
    @Override
    public String getUniqueKey() {
        //Only one thread is allowed to run on a single sha1 value so we don't do double calculations
        return sha1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChecksumCalculationWorkItem)) {
            return false;
        }
        ChecksumCalculationWorkItem workItem = (ChecksumCalculationWorkItem) o;
        return Objects.equals(getSha1(), workItem.getSha1());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSha1());
    }
}

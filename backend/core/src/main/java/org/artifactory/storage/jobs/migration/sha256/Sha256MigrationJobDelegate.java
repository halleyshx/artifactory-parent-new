package org.artifactory.storage.jobs.migration.sha256;


import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * Passed as delegate to {@link org.artifactory.api.repo.RepositoryService} to avoid polluting it with logic that
 * belongs in this job but must run async via a service.
 *
 * @author Dan Feldman
 */
public interface Sha256MigrationJobDelegate {

    void markFatalErrorOnPaths(Collection<RepoPath> paths, String error);

    void incrementTotalDone();

    void incrementCurrentBatchCount();

    Logger log();

    /**
     * Tries to retrieve the sha2 value matching this {@param sha1} from the binaries table, if it doesn't exist
     * the binary matching the sha1 value is retrieved from the binarystore and the sha2 is calculated from the
     * incoming stream.
     *
     * @throws Sha256CalculationFatalException on any fatal error that requires user intervention.
     */
    String getOrCalculateSha2(String sha1) throws Sha256CalculationFatalException;
}

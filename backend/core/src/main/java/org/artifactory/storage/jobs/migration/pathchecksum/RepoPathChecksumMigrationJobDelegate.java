package org.artifactory.storage.jobs.migration.pathchecksum;

import org.slf4j.Logger;

/**
 * @author gidis
 */
public interface RepoPathChecksumMigrationJobDelegate {

    void incrementTotalDone();

    void incrementCurrentBatchCount();

    Logger log();

}


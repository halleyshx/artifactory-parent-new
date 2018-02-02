package org.artifactory.storage.db;

import org.artifactory.api.repo.Async;

/**
 * @author Dan Feldman
 */
public interface InternalDbService extends DbService {

    void initDb() throws Exception;

    /**
     * TO BE USED ONLY BY THE SHA256 MIGRATION JOB
     */
    boolean verifySha256State();

    /**
     * Signifies the sha256 columns exists in appropriate db tables, and this instance can write their values into db.
     */
    boolean isSha256Ready();

    boolean verifyUniqueRepoPathChecksumState();

    boolean isUniqueRepoPathChecksumReady();

    @Async
    void verifyMigrations();
}

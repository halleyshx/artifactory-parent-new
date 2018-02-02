package org.artifactory.storage.db.security.service;

/**
 * @author gidis
 */
public class VersioningCacheException extends RuntimeException {

    public VersioningCacheException(String messahe, Exception e) {
        super(messahe,e);
    }
}

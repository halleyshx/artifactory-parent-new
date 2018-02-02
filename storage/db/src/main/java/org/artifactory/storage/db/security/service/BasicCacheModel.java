package org.artifactory.storage.db.security.service;

/**
 * @author gidis
 */
public interface BasicCacheModel {

    long getVersion();

    void setVersion(long version);

    /**
     * This method is called every cache update call on an old cache, after replacing this cache with a newer cache.
     */
    void destroy();
}

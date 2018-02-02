package org.artifactory.storage;

/**
 * @author gidis
 */
public enum BinaryProviderType {
    filesystem, // binaries are stored in the filesystem
    fullDb,     // binaries are stored as blobs in the db, filesystem is used for caching unless cache size is 0
    cachedFS,   // binaries are stored in the filesystem, but a front cache (faster access) is added
    S3,         // binaries are stored in S3 JClouds API
    S3Old,        // binaries are stored in S3 Jets3t API
    goog        // binaries are stored in S3 Jets3t API
}

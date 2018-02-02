package org.artifactory.api.storage;

/**
 * This is an adapter class to support mission control user plugin. We need this because we moved the original
 * {@link org.jfrog.storage.common.StorageUnit} to another location, and mission control plugin should support both
 * Artifactory 4.x and 5.x, without having different plugins per version with different imports on each plugin.
 *
 * Do not use this class directly, use {@link org.jfrog.storage.common.StorageUnit} instead.
 *
 * @author Shay Bagants
 */
@Deprecated
public class StorageUnit {

    /**
     * Convert the number of bytes to a human readable size, if the size is more than 1024 megabytes display the correct
     * number of gigabytes.
     *
     * @param size The size in bytes.
     * @return The size in human readable format.
     *
     * @deprecated as of Artifactory 5.0, replaces by {@link org.jfrog.storage.common.StorageUnit#toReadableString(long)}
     */
    @Deprecated
    public static String StorageUnit(long size) {
        return org.jfrog.storage.common.StorageUnit.toReadableString(size);
    }
}


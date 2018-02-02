package org.artifactory.search.aql;

import java.io.Closeable;
import java.util.Map;

/**
 * Iterable AQL search result interface for the Artifactory public API.
 *
 * @author Shay Bagants
 */
public interface AqlResult extends Iterable<Map<String, Object>>, Closeable {

    /**
     * The offset from the first record from which to return results
     *
     * @return The offset from the first record from which to return results
     */
    Long getStart();

    /**
     * The total number of results
     *
     * @return The total number of results
     */
    Long getTotal();

    /**
     * The limit value
     *
     * @return The limit value
     */
    Long getLimited();
}

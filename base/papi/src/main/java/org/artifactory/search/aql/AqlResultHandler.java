package org.artifactory.search.aql;

/**
 * AQL result handler interface
 *
 * @author Shay Bagants
 */
public interface AqlResultHandler {

    void handle(AqlResult result);

}
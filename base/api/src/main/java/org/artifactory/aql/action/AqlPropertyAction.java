package org.artifactory.aql.action;

import java.util.List;

/**
 * Aql Actions that assign new values should implement this interface.
 *
 * @author Dan Feldman
 */
public interface AqlPropertyAction extends AqlAction {

    List<String> getKeys();

    void addKey(String key);

    String getValue();

    void setValue(String newValue);
}

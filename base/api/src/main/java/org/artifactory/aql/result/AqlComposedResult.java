package org.artifactory.aql.result;

import org.artifactory.aql.model.AqlFieldEnum;

import java.util.List;

/**
 * @author Shay Bagants
 */
public interface AqlComposedResult extends AqlLazyResult {

    List<AqlFieldEnum> getOriginalFields();
}

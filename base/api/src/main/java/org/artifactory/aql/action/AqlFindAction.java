package org.artifactory.aql.action;

import org.artifactory.aql.model.AqlActionEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.result.AqlRestResult.Row;

/**
 * The find action actually does nothing, every aql query is first and foremost a find query - so it simply returns the
 * current row to the caller.
 *
 * @author Dan Feldman
 */
public class AqlFindAction implements AqlAction {

    @Override
    public Row doAction(Row row) throws AqlActionException {
        // Nothing to do for the find action.
        return row;
    }

    @Override
    public String getName() {
        return AqlActionEnum.find.name;
    }


    @Override
    public boolean supportsDomain(AqlDomainEnum domain) {
        // find is supported on all domains
        return true;
    }

    @Override
    public void setDryRun(boolean dryRun) {
        //nop
    }

    @Override
    public boolean isDryRun() {
        return false;
    }
}

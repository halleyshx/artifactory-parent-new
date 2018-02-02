package org.artifactory.aql.action;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.model.AqlActionEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.result.AqlRestResult.Row;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.common.StatusHolder;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.artifactory.aql.action.AqlActionException.Reason.*;

/**
 * Deletes the item denoted by a given row by resolving it's path and undeploying it using the {@link RepositoryService}
 *
 * @author Dan Feldman
 */
public class AqlDeleteItemAction implements AqlAction {
    private static final Logger log = LoggerFactory.getLogger(AqlDeleteItemAction.class);

    private boolean dryRun = true;

    @Override
    public Row doAction(Row row) throws AqlActionException {
        //Should also be verified in query construction phase but just in case.
        if (!AqlDomainEnum.items.equals(row.getDomain())) {
            String msg = "Skipping delete action for row, only items domain is supported - row has domain: " + row.getDomain();
            log.debug(msg);
            throw new AqlActionException(msg, UNSUPPORTED_FOR_DOMAIN);
        }
        doIfNeeded(row);
        return row;
    }

    private void doIfNeeded(Row row) throws AqlActionException {
        RepoPath itemPath = AqlUtils.fromAql(row);
        if (dryRun) {
            if (!getAuthService().canDelete(itemPath)) {
                throw new AqlActionException("User does not have permission to delete item at '"
                        + itemPath.toPath() + "'.", ACTION_FAILED);
            }
        } else {
            // Permissions are verified by repoService.
            StatusHolder status = getRepoService().undeployMultiTransaction(itemPath);
            if (status.isError()) {
                throw new AqlActionException(status.getLastError().getMessage(), ACTION_FAILED);
            }
        }
    }

    @Override
    public String getName() {
        return AqlActionEnum.deleteItem.name;
    }

    @Override
    public boolean supportsDomain(AqlDomainEnum domain) {
        return AqlDomainEnum.items.equals(domain);
    }

    @Override
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public boolean isDryRun() {
        return dryRun;
    }

    private RepositoryService getRepoService() {
        return ContextHelper.get().beanForType(RepositoryService.class);
    }

    private AuthorizationService getAuthService() {
        return ContextHelper.get().beanForType(AuthorizationService.class);
    }
}

package org.artifactory.storage.db.aql.sql.result;

import org.artifactory.aql.action.AqlAction;
import org.artifactory.aql.model.*;
import org.artifactory.aql.result.AqlComposedResult;
import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper for AqlLazyResult including the original Aql query, the extension query and the merged query.
 * This class is used when merging two aql queries. When we merge two queries, during the result processing, we might
 * need some data from the original/extension queries, this object can provide this data.
 *
 * @author Shay Bagants
 */
public class AqlComposedResultImpl implements AqlComposedResult {
    private AqlLazyResult aqlLazyResult;
    private AqlQuery originalMainQueryAqlQuery;
    private AqlQuery extensionAqlQuery;
    private AqlQuery merge;

    public AqlComposedResultImpl(AqlLazyResult aqlLazyResult,
            AqlQuery originalMainQueryAqlQuery,
            AqlQuery extentionAqlQuery,
            AqlQuery merge) {
        this.aqlLazyResult = aqlLazyResult;
        this.originalMainQueryAqlQuery = originalMainQueryAqlQuery;
        this.extensionAqlQuery = extentionAqlQuery;
        this.merge = merge;
    }

    @Override
    public AqlPermissionProvider getPermissionProvider() {
        return aqlLazyResult.getPermissionProvider();
    }

    @Override
    public AqlRepoProvider getRepoProvider() {
        return aqlLazyResult.getRepoProvider();
    }

    @Override
    public List<DomainSensitiveField> getFields() {
        return aqlLazyResult.getFields();
    }

    @Override
    public ResultSet getResultSet() {
        return aqlLazyResult.getResultSet();
    }

    @Override
    public long getLimit() {
        return aqlLazyResult.getLimit();
    }

    @Override
    public long getOffset() {
        return aqlLazyResult.getOffset();
    }

    @Override
    public AqlDomainEnum getDomain() {
        return aqlLazyResult.getDomain();
    }

    @Override
    public AqlAction getAction() {
        return aqlLazyResult.getAction();
    }

    @Override
    public void close() throws Exception {
        aqlLazyResult.close();
    }

    public AqlQuery getOriginalMainQueryAqlQuery() {
        return originalMainQueryAqlQuery;
    }

    public AqlQuery getExtensionAqlQuery() {
        return extensionAqlQuery;
    }

    public AqlQuery getMerge() {
        return merge;
    }

    @Override
    public List<AqlFieldEnum> getOriginalFields() {
        List<DomainSensitiveField> resultFields = originalMainQueryAqlQuery.getResultFields();
        return resultFields.stream().map(DomainSensitiveField::getField).collect(Collectors.toList());
    }
}

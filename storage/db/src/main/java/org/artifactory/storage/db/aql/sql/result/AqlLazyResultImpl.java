/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.storage.db.aql.sql.result;

import org.artifactory.aql.action.AqlAction;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.model.AqlRepoProvider;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Gidi Shabat
 */
public class AqlLazyResultImpl implements AqlLazyResult {
    private static final Logger log = LoggerFactory.getLogger(AqlLazyResultImpl.class);

    private final long limit;
    private final long offset;
    private final List<DomainSensitiveField> fields;
    private final ResultSet resultSet;
    private final AqlDomainEnum domain;
    private final AqlAction action;
    private final AqlPermissionProvider aqlPermissionProvider;
    private final AqlRepoProvider aqlRepoProvider;

    public AqlLazyResultImpl(ResultSet resultSet, SqlQuery sqlQuery, AqlPermissionProvider aqlPermissionProvider, AqlRepoProvider aqlRepoProvider) {
        this.aqlPermissionProvider = aqlPermissionProvider;
        this.aqlRepoProvider = aqlRepoProvider;
        this.resultSet = resultSet;
        limit = sqlQuery.getLimit();
        offset = sqlQuery.getOffset();
        fields = sqlQuery.getResultFields();
        domain = sqlQuery.getDomain();
        action = sqlQuery.getAction();
    }

    @Override
    public AqlPermissionProvider getPermissionProvider() {
        return aqlPermissionProvider;
    }

    @Override
    public AqlRepoProvider getRepoProvider() {
        return aqlRepoProvider;
    }

    @Override
    public List<DomainSensitiveField> getFields() {
        return fields;
    }

    @Override
    public ResultSet getResultSet() {
        return resultSet;
    }

    @Override
    public long getLimit() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public AqlDomainEnum getDomain() {
        return domain;
    }

    @Override
    public AqlAction getAction() {
        return action;
    }

    @Override
    public void close()  {
        try {
            if(resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            log.error("Failed to close AQL result: ", e);
        }
    }
}

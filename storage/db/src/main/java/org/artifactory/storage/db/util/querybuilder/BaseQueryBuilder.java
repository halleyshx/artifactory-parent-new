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

package org.artifactory.storage.db.util.querybuilder;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author Chen Keinan
 */
abstract class BaseQueryBuilder implements IQueryBuilder {

    @Override
    public String build(String distinct, String fields, String tables, String joinType, String joinTable,
            String joinOn, String conditions, String orderBy, String groupBy, Long offSet, Long limit) {
        StringBuilder builder = new StringBuilder();
        // Start select section
        builder.append("select ");
        // set distinct
        if (!isBlank(distinct)) {
            builder.append("distinct ");
        }
        // Start append the fields section
        if (isBlank(fields)) {
            builder.append("* ");
        } else {
            builder.append(fields);
        }
        // Set from section
        builder.append("from ");
        if (isBlank(tables)) {
            throw new RuntimeException("Failed to build sql query. Reason: missing tables for the from section");
        } else {
            builder.append(tables);
        }
        // Set join section
        if (joinType != null && !isBlank(joinTable) && !isBlank(joinOn)) {
            builder.append(joinType).append(" join ");
            builder.append(joinTable);
            builder.append("on ");
            builder.append(joinOn);
        }
        // Set condition section
        if (!isBlank(conditions)) {
            builder.append("where ");
            builder.append(conditions);
        }
        // Set order section
        if (shouldAddOrderBy(offSet, limit) && !isBlank(orderBy)) {
            builder.append("order by ");
            builder.append(orderBy);
        }
        // Set group by section
        if (!isBlank(groupBy)) {
            builder.append("group by ");
            builder.append(groupBy);
        }
        String result;
        // handle special pagination behavior
        if (isPagination(offSet, limit)) {
            // Need special db type behaviour
            if (offSet == null || offSet < 0) { // normalize
                offSet = 0l;
            }
            if (limit == null || limit < 0) { // normalize
                limit = Long.MAX_VALUE;
            }
            result = uniqueBuild(builder.toString(), orderBy, offSet, limit);
        } else {
            result = builder.toString();
        }
        return result;
    }

    abstract String uniqueBuild(String baseQuery, String sortBy, long offSet, long limit);

    abstract boolean shouldAddOrderBy(Long offSet, Long limit);

    boolean isPagination(Long offSet, Long limit) {
        return offSet != null && offSet > 0 || limit != null && limit < Long.MAX_VALUE;
    }
}


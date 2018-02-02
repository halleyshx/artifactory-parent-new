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

import static org.artifactory.storage.db.util.querybuilder.QueryBuilderUtils.addButLimit;

/**
 * @author Chen Keinan
 */
public class OracleQueryBuilder extends BaseQueryBuilder {

    @Override
    public String uniqueBuild(String baseQuery, String sortBy, long offSet, long limit) {

        long maxToFetch = addButLimit(offSet, limit, Long.MAX_VALUE);
        StringBuilder builder;
        builder = new StringBuilder();
        builder.append("select * from( ");
        builder.append("select rownum rnum, inner_query.* from ( ").append(baseQuery).append(") inner_query ");
        builder.append("where ROWNUM <= ").append(maxToFetch).append(")");
        builder.append("where rnum > ").append(offSet).append(" ");
        return builder.toString();
    }

    @Override
    public boolean shouldAddOrderBy(Long offSet, Long limit) {
        return true;
    }
}

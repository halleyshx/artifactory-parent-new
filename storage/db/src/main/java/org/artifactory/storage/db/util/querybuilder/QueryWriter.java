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

import org.artifactory.api.context.ContextHelper;

/**
 * @author Chen Keinan
 */
public class QueryWriter {

    private final IQueryBuilder queryBuilder;
    private String distinct;
    private String fields;
    private String tables;
    private JoinType joinType;
    private String joinTables;
    private String joinOn;
    private String conditions;
    private String groupBy;
    private String orderBy;
    private Long offSet;
    private Long limit;

    public QueryWriter() {
        queryBuilder = ContextHelper.get().beanForType(IQueryBuilder.class);
    }

    public QueryWriter distinct() {
        this.distinct = "distinct ";
        return this;
    }

    public QueryWriter select() {
        this.fields = " * ";
        return this;
    }

    public QueryWriter select(String fields) {
        this.fields = fields;
        return this;
    }

    public QueryWriter from(String tables) {
        this.tables = tables;
        return this;
    }

    public QueryWriter innerJoin(String joinTable, String joinOn) {
        this.joinType = JoinType.INNER;
        this.joinTables = joinTable;
        this.joinOn = joinOn;
        return this;
    }

    public QueryWriter leftJoin(String joinTable, String joinOn) {
        this.joinType = JoinType.LEFT;
        this.joinTables = joinTable;
        this.joinOn = joinOn;
        return this;
    }

    public QueryWriter groupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public QueryWriter where(String condition) {
        this.conditions = condition;
        return this;
    }

    public QueryWriter orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public QueryWriter offset(Long offset) {
        offSet = offset;
        return this;
    }

    public QueryWriter limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public String build() {
        return queryBuilder.build(distinct, fields, tables, joinType != null ? joinType.name() : null, joinTables, joinOn, conditions, orderBy,
                groupBy, offSet, limit);
    }

    public void clear() {
        distinct = null;
        fields = null;
        tables = null;
        joinType = null;
        joinTables = null;
        joinOn = null;
        conditions = null;
        groupBy = null;
        orderBy = null;
        offSet = null;
        limit = null;
    }

    public enum JoinType {
        INNER("inner"),
        LEFT("left");

        final String joinType;

        JoinType(String joinType) {
            this.joinType = joinType;
        }
    }

}


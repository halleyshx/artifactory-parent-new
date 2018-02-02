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

package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.action.AqlAction;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;

import java.util.List;
import java.util.Stack;

/**
 * The context is being used by the AqlApiToAqlQuery converter and the Parser to AqlQuery converter
 * It contains the AqlQuery that is being filled and other information needed to build the AqlQuery
 *
 * @author Gidi Shabat
 */
public class AdapterContext {

    private AqlQuery aqlQuery = new AqlQuery();
    private int tableId = SqlTable.MINIMAL_DYNAMIC_TABLE_ID;
    private Stack<AqlQueryElement> functions = new Stack<>();

    public void push(AqlQueryElement aqlQueryElement) {
        functions.push(aqlQueryElement);
    }

    public void addAqlQueryElements(AqlQueryElement aqlQueryElement) {
        aqlQuery.getAqlElements().add(aqlQueryElement);
    }

    public AqlQueryElement peek() {
        return functions.peek();
    }

    public AqlQueryElement pop() {
        return functions.pop();
    }

    public int provideIndex() {
        int temp = tableId;
        tableId = tableId + 1;
        return temp;
    }

    public List<AqlQueryElement> getAqlQueryElements() {
        return aqlQuery.getAqlElements();
    }

    public void addField(DomainSensitiveField field) {
        if (!aqlQuery.getResultFields().contains(field)) {
            aqlQuery.getResultFields().add(field);
        }
    }

    public Stack<AqlQueryElement> getFunctions() {
        return functions;
    }

    public AqlQuery getAqlQuery() {
        return aqlQuery;
    }

    public List<DomainSensitiveField> getResultFields() {
        return aqlQuery.getResultFields();
    }

    public AqlDomainEnum getDomain() {
        return aqlQuery.getDomain();
    }

    public void setDomain(AqlDomainEnum domainEnum) {
        aqlQuery.setDomain(domainEnum);
    }

    public void setSort(SortDetails sortDetails) {
        aqlQuery.setSort(sortDetails);
    }

    public void setLimit(long limit) {
        aqlQuery.setLimit(limit);
    }

    public void setOffset(long offset) {
        aqlQuery.setOffset(offset);
    }

    public void setAction(AqlAction action) {
        aqlQuery.setAction(action);
    }
}

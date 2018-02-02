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

package org.artifactory.storage.db.aql.service.decorator;

import com.google.common.collect.Lists;
import org.artifactory.aql.AqlFieldResolver;
import org.artifactory.aql.model.*;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.storage.db.aql.sql.builder.links.TableLink;
import org.artifactory.storage.db.aql.sql.builder.query.aql.*;
import org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;

import java.util.List;

import static org.artifactory.aql.model.AqlPhysicalFieldEnum.itemRepo;

/**
 * @author Shay Yaakov
 */
public class TrashcanDecorator implements DecorationStrategy {

    @Override
    public void decorate(AqlQuery aqlQuery, AqlQueryDecoratorContext decoratorContext) {
        filterTrashcanIfNeeded(aqlQuery);
    }

    private void filterTrashcanIfNeeded(AqlQuery aqlQuery) {
        if (aqlQuery.getAqlElements().isEmpty()) {
            return;
        }
        if (AqlDomainEnum.items.equals(aqlQuery.getDomain())) {
            decorateItemsSearch(aqlQuery);
        } else if (AqlDomainEnum.properties.equals(aqlQuery.getDomain())) {
            decoratePropertiesSearch(aqlQuery);
        }
    }

    private void decorateItemsSearch(AqlQuery aqlQuery) {
        if (!repoEqualsFilterFound(aqlQuery)) {
            // (query)AND(repo != "auto-trashcan")
            AqlField itemRepo = AqlFieldResolver.resolve(AqlPhysicalFieldEnum.itemRepo);
            AqlVariable trashRepo = AqlFieldResolver.resolve(TrashService.TRASH_KEY, AqlVariableTypeEnum.string);
            TableLink nodesTable = AqlTableGraph.tablesLinksMap.get(SqlTableEnum.nodes);
            SimpleCriterion criteria = new SimpleCriterion(Lists.newArrayList(AqlDomainEnum.properties), itemRepo,
                    nodesTable.getTable(), AqlComparatorEnum.notEquals.signature, trashRepo, nodesTable.getTable(), false);

            List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
            aqlElements.add(0, AqlAdapter.open);
            aqlElements.add(AqlAdapter.close);
            aqlElements.add(AqlAdapter.and);
            aqlElements.add(criteria);
        }
    }

    private void decoratePropertiesSearch(AqlQuery aqlQuery) {
        // (query)AND(trash.time not exists)
        AqlField key = AqlFieldResolver.resolve(AqlPhysicalFieldEnum.propertyKey);
        AqlVariable trashTime = AqlFieldResolver.resolve(TrashService.PROP_TRASH_TIME, AqlVariableTypeEnum.string);
        TableLink propsTable = AqlTableGraph.tablesLinksMap.get(SqlTableEnum.node_props);
        SimplePropertyCriterion criteria = new SimplePropertyCriterion(Lists.newArrayList(AqlDomainEnum.properties),
                key, propsTable.getTable(), AqlComparatorEnum.notEquals.signature, trashTime, propsTable.getTable(),false);

        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        aqlElements.add(0, AqlAdapter.open);
        aqlElements.add(AqlAdapter.close);
        aqlElements.add(AqlAdapter.and);
        aqlElements.add(criteria);
    }

    private boolean repoEqualsFilterFound(AqlQuery aqlQuery) {
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        for (AqlQueryElement aqlQueryElement : aqlElements) {
            if (aqlQueryElement instanceof SimpleCriterion) {
                SimpleCriterion criteria = (SimpleCriterion) aqlQueryElement;
                if (criteriaIsRepoEquals(criteria)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean criteriaIsRepoEquals(SimpleCriterion criteria) {
        AqlField field = (AqlField) criteria.getVariable1();
        boolean fieldIsRepo = itemRepo == field.getFieldEnum();
        boolean comparatorIsEquals = AqlComparatorEnum.equals.equals(AqlComparatorEnum.value(criteria.getComparatorName()));
        return fieldIsRepo && comparatorIsEquals;
    }
}

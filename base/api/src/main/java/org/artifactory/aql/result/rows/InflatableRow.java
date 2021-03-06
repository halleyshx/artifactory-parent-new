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

package org.artifactory.aql.result.rows;

import com.google.common.collect.Maps;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlPhysicalFieldEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.AqlRestResult;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 *         The class represent single row that returns from the database. in addition the InflatableRow contains the domin sensitive
 *         fields that corelates the results and allows to inflate the flate row result into multi domain model
 */
public class InflatableRow implements RowResult {
    private Map<DomainSensitiveField, Object> map = Maps.newHashMap();

    /**
     * pushes the pair: {DomainSensitiveField and results} to the InflatableRow
     */
    @Override
    public void put(DomainSensitiveField field, Object value) {
        map.put(field, value);
    }

    @Override
    public Object get(DomainSensitiveField field) {
        return map.get(field);
    }

    /**
     * The method transforms the flat row into multi domain model
     * The result is multi layer map (each layer represent single domain)
     */
    public Map<String, AqlRestResult.Row> inflate() {
        Map<String, AqlRestResult.Row> result = Maps.newHashMap();
        AqlRestResult.Row prev = null;
        // For each field in the row resolve the domains list and set the value in its location in the multi layer domain
        for (DomainSensitiveField field : map.keySet()) {
            Map<String, AqlRestResult.Row> current = result;
            List<AqlDomainEnum> subDomains = field.getSubDomains();
            for (AqlDomainEnum subDomain : subDomains) {
                String id = resolveId(subDomain);
                // if map exist use it else create it
                if (current == null) {
                    current = Maps.newHashMap();
                    prev.subDomains = current;
                }
                // if element row exist, use it,else create it
                AqlRestResult.Row row = current.get(id);
                if (row == null) {
                    row = new AqlRestResult.Row(subDomain);
                    current.put(id, row);
                    fillRelevantFields(row, subDomain);
                }
                prev = row;
                current = row.subDomains;
            }
        }
        // Clean empty rows
        clean(result.values().iterator().next());
        return result;
    }

    /**
     * Cleans all empty row (no need for them)
     */
    private boolean clean(AqlRestResult.Row next) {
        // Clean all sub-domains
        if (next.subDomains != null) {
            Iterator<AqlRestResult.Row> iterator = next.subDomains.values().iterator();
            while (iterator.hasNext()) {
                AqlRestResult.Row row = iterator.next();
                if (clean(row)) {
                    iterator.remove();
                }
            }
        }
        // Check if we should clean this row parent
        try {
            Field[] declaredFields = next.getClass().getFields();
            for (Field declaredField : declaredFields) {
                if (!declaredField.getName().equals("subDomains") && !declaredField.getName().equals("domain")) {
                    declaredField.setAccessible(true);
                    Object value = declaredField.get(next);
                    //Special behaviour for archive domain, since archive domain is built by two tables and doesn't have real key
                    if (value != null && !"archiveId".equals(declaredField.getName()) && !AqlPhysicalFieldEnum.valueOf(
                            declaredField.getName()).isId()) {
                        return false;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AqlException("Failed to map result fields", e);
        }
        return next.subDomains == null || next.subDomains.size() == 0;
    }

    /**
     * The method fills all data (result from db) that is relevant for the current domain
     */
    private boolean fillRelevantFields(AqlRestResult.Row row, AqlDomainEnum subDomain) {
        boolean containsData = false;
        for (DomainSensitiveField field : map.keySet()) {
            Object value = map.get(field);
            if (field.getField().getDomain() == subDomain) {
                if (value != null && !field.getField().isId()) {
                    containsData = true;
                }

                row.put(field.getField().getName(), value);
            }
        }
        return containsData;
    }

    /**
     * The method returns the element id according to the domain
     */
    private String resolveId(AqlDomainEnum domain) {
        StringBuilder builder = new StringBuilder();
        for (DomainSensitiveField domainSensitiveField : map.keySet()) {
            AqlDomainEnum fieldDomain = domainSensitiveField.getField().getDomain();
            if (fieldDomain == domain) {
                String elementKey = domainSensitiveField.getField().getName();
                Object value = map.get(domainSensitiveField);
                builder.append("((key:").append(elementKey).append(")(value:").append(toString(value)).append("))");
            }
        }
        return builder.toString();
    }

    private String toString(Object value) {
        if (value != null && value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        } else {
            return String.valueOf(value);
        }
    }

}

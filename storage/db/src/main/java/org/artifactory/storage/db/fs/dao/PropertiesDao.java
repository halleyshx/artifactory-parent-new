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

package org.artifactory.storage.db.fs.dao;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.fs.entity.NodeProperty;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.JdbcHelper;
import org.jfrog.storage.DbType;
import org.jfrog.storage.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A data access object for the properties table.
 *
 * @author Yossi Shaul
 */
@Repository
public class PropertiesDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(PropertiesDao.class);
    private static final int PROP_VALUE_MAX_SIZE = 4000;
    private static final int PROP_VALUE_MSSQL_MAX_SIZE = 900; //limit in MSSQL the maximum length of properties value
    // to 900 characters , since MSSQL cannot have index more than 900 characters

    @Autowired
    private DbService dbService;

    @Autowired
    public PropertiesDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public boolean hasNodeProperties(long nodeId) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT COUNT(1) FROM node_props WHERE node_id = ?", nodeId);
            if (resultSet.next()) {
                int propsCount = resultSet.getInt(1);
                return propsCount > 0;
            }
            return false;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public List<NodeProperty> getNodeProperties(long nodeId) throws SQLException {
        ResultSet resultSet = null;
        List<NodeProperty> results = Lists.newArrayList();
        try {
            // the child path must be the path+name of the parent
            resultSet = jdbcHelper.executeSelect("SELECT * FROM node_props WHERE node_id = ?", nodeId);
            while (resultSet.next()) {
                results.add(propertyFromResultSet(resultSet));
            }
            return results;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    /**
     * This query is meant to return all properties of nodes that have property key {@param propKey} and values that are
     * in {@param propValues} - for instance, give me all properties of all artifacts that have property bower.name
     * with values 'jquery' or 'bootstrap'. results are limited to artifact that reside in {@param repo}
     * @throws SQLException
     */
    public Multimap<Long, NodeProperty> getNodesProperties(String repo, String propKey, List<String> propValues) throws SQLException {
        Multimap<Long, NodeProperty> results = HashMultimap.create();
        if (CollectionUtils.isEmpty(propValues)) {
            return results;
        }
        // Oracle limits the max elements in the IN clause to 1000. Lists bigger than max chunk value are done in multiple queries
        final int CHUNK = ConstantValues.propertiesSearchChunkSize.getInt();
        // split to chunks of no more than CHUNK
        for (int i = 0; i < propValues.size(); i += CHUNK) {
            int chunkMaxIndex = Math.min(i + CHUNK, propValues.size());
            List<String> chunk = propValues.subList(i, chunkMaxIndex);
            String allPropsQuery =
                    "SELECT p1.prop_id, p1.node_id, p1.prop_key, p1.prop_value " +
                            "FROM node_props p INNER JOIN node_props p1 ON p.node_id = p1.node_id INNER JOIN nodes n " +
                            "ON n.node_id = p.node_id " +
                            "WHERE n.node_type = 1 AND n.repo = ? AND p.prop_key = ? AND p.prop_value IN (#)";
            try (ResultSet resultSet = jdbcHelper.executeSelect(allPropsQuery, repo, propKey, chunk)) {
                while (resultSet.next()) {
                    NodeProperty nodeProperty = propertyFromResultSet(resultSet);
                    results.put(nodeProperty.getNodeId(), nodeProperty);
                }
            }
        }
        return results;
    }


    public int deleteNodeProperties(long nodeId) throws SQLException {
        return jdbcHelper.executeUpdate("DELETE FROM node_props WHERE node_id = ?", nodeId);
    }

    public int delete(NodeProperty property) throws SQLException {
        return jdbcHelper.executeUpdate("DELETE FROM node_props WHERE prop_id = ? AND prop_key = ?",
                property.getPropId(), property.getPropKey());
    }

    public int updateValue(NodeProperty property) throws SQLException {
        String propValue = getPropertyValueEnforceLength(property);
        return jdbcHelper.executeUpdate("UPDATE node_props SET prop_value = ? WHERE prop_id = ? AND prop_key = ?",
                propValue, property.getPropId(), property.getPropKey());
    }

    public int create(NodeProperty property) throws SQLException {
        String propValue = getPropertyValueEnforceLength(property);
        return jdbcHelper.executeUpdate("INSERT INTO node_props " +
                        "(prop_id, node_id, prop_key, prop_value) " +
                        "VALUES(?, ?, ?, ?)",
                property.getPropId(), property.getNodeId(), property.getPropKey(), propValue);
    }

    private String getPropertyValueEnforceLength(NodeProperty property) {
        String propValue = nullIfEmpty(property.getPropValue());
        int maxPropValue = PROP_VALUE_MAX_SIZE;
        if(dbService.getDatabaseType().equals(DbType.MSSQL)){
            maxPropValue = PROP_VALUE_MSSQL_MAX_SIZE;
        }
        if (propValue != null && propValue.length() > maxPropValue) {
            log.info("Trimming property value to {} characters '{}'", maxPropValue,property.getPropKey());
            log.debug("Trimming property value to {} characters {}: {}", maxPropValue, property.getPropKey(), property.getPropValue());
            propValue = StringUtils.substring(propValue, 0, maxPropValue);
        }
        return propValue;
    }

    private NodeProperty propertyFromResultSet(ResultSet resultSet) throws SQLException {
        long propId = resultSet.getLong(1);
        long nodeId = resultSet.getLong(2);
        String propKey = resultSet.getString(3);
        String propValue = emptyIfNull(resultSet.getString(4));
        return new NodeProperty(propId, nodeId, propKey, propValue);
    }
}

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

package org.artifactory.storage.db.fs.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.artifactory.common.ConstantValues;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.model.xstream.fs.PropertiesImpl.PropertyChange;
import org.artifactory.model.xstream.fs.PropertiesImpl.PropertyChangeResult;
import org.artifactory.model.xstream.fs.PropertyWithId;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.fs.dao.PropertiesDao;
import org.artifactory.storage.db.fs.entity.NodeProperty;
import org.artifactory.storage.fs.service.FileService;
import org.artifactory.storage.fs.service.PropertiesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Yossi Shaul
 */
@Service
public class DbPropertiesServiceImpl implements PropertiesService {

    private static final Logger log = LoggerFactory.getLogger(DbPropertiesServiceImpl.class);

    @Autowired
    private DbService dbService;

    @Autowired
    private PropertiesDao propertiesDao;

    @Autowired
    private FileService fileService;

    private boolean replacePropertiesOnSet = false;
    private boolean logSetPropertiesPerformance = false;

    @PostConstruct
    private void init() {
        replacePropertiesOnSet = ConstantValues.nodePropertiesReplaceAll.getBoolean();
        logSetPropertiesPerformance = ConstantValues.nodePropertiesLogPerformance.getBoolean();
    }

    @Nonnull
    @Override
    public Properties getProperties(ItemInfo itemInfo) {
        long id = itemInfo.getId();
        if (id < 0) {
            return getProperties(itemInfo.getRepoPath());
        } else {
            return getPropertiesById(id);
        }
    }

    @Override
    @Nonnull
    public Properties getProperties(RepoPath repoPath) {
        long nodeId = fileService.getNodeId(repoPath);
        return getPropertiesById(nodeId);
    }

    @Nonnull
    private Properties getPropertiesById(long nodeId) {
        if (nodeId > 0) {
            return loadProperties(nodeId);
        } else {
            return new PropertiesImpl();
        }
    }

    @Override
    public Map<Long, Properties> getAllProperties(String repoKey, String propKey, List<String> propValues) {
        Map<Long, Properties> nodesProps = Maps.newHashMap();
        Multimap<Long, NodeProperty> queryResults;
        try {
            queryResults = propertiesDao.getNodesProperties(repoKey, propKey, propValues);
        } catch (SQLException e) {
            throw new StorageException("Failed to load properties", e);
        }
        for (Map.Entry<Long, Collection<NodeProperty>> entry : queryResults.asMap().entrySet()) {
            Properties props = new PropertiesImpl();
            entry.getValue().forEach(singleProp -> props.put(singleProp.getPropKey(), singleProp.getPropValue()));
            nodesProps.put(entry.getKey(), props);
        }
        return nodesProps;
    }

    @Nonnull
    private Properties loadProperties(long nodeId) {
        try {
            List<NodeProperty> nodeProperties = propertiesDao.getNodeProperties(nodeId);
            return new PropertiesImpl(nodeId, nodeProperties);
        } catch (SQLException e) {
            throw new StorageException("Failed to load properties for " + nodeId, e);
        }
    }

    @Override
    public boolean hasProperties(RepoPath repoPath) {
        long nodeId = fileService.getNodeId(repoPath);
        try {
            return nodeId > 0 && propertiesDao.hasNodeProperties(nodeId);
        } catch (SQLException e) {
            throw new StorageException("Failed to check properties for " + repoPath, e);
        }
    }

    /**
     * Sets properties
     *
     * @param nodeId     Id of the node to set properties on
     * @param properties The properties to set
     */
    @Override
    public void setProperties(long nodeId, Properties properties) {
        try {
            PropertiesImpl propsImpl = null;
            long start = System.currentTimeMillis();
            if (!replacePropertiesOnSet) {
                propsImpl = properties instanceof PropertiesImpl ? (PropertiesImpl) properties : null;
            }
            if (propsImpl != null && propsImpl.isChangeTrackingEnabled() && propsImpl.getNodeId() == nodeId) {
                updateProperties(nodeId, propsImpl);
            } else {
                repalceProperties(nodeId, properties);
            }
            if (logSetPropertiesPerformance) {
                long elapsed = System.currentTimeMillis() - start;
                log.info("Set properties of node {} with {} properties took {} ms", nodeId, properties.size(), elapsed);
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to set properties on node: " + nodeId, e);
        }
    }

    private void updateProperties(long nodeId, PropertiesImpl properties) throws SQLException {
        log.trace("Updating properties of node {} using tracked changes", nodeId);
        Collection<PropertyChange> propertyChanges = properties.getPropertyChanges();
        log.debug("Found {} tracked changes for node {}", propertyChanges.size(), nodeId);
        List<PropertyChangeResult> changeResults = Lists.newArrayList();
        for (PropertyChange change : propertyChanges) {
            PropertyWithId changeProperty = change.getProperty();
            NodeProperty resultProperty = null;
            boolean success;
            switch (change.getChangeType()) {
                case CREATE:
                    resultProperty = new NodeProperty(dbService.nextId(), nodeId,
                            changeProperty.getPropKey(), changeProperty.getPropValue());
                    success = propertiesDao.create(resultProperty) > 0;
                    break;
                case UPDATE:
                    resultProperty = new NodeProperty(changeProperty.getPropId(), nodeId,
                            changeProperty.getPropKey(), changeProperty.getPropValue());
                    success = propertiesDao.updateValue(resultProperty) > 0;
                    break;
                case DELETE:
                    NodeProperty propToDelete = new NodeProperty(changeProperty.getPropId(), nodeId,
                            changeProperty.getPropKey(), changeProperty.getPropValue());
                    success = propertiesDao.delete(propToDelete) > 0;
                    break;
                default:
                    throw new IllegalStateException("Unexpected change type: " + change.getChangeType());
            }
            if (log.isDebugEnabled()) {
                long propId = resultProperty == null ? changeProperty.getPropId() : resultProperty.getPropId();
                log.debug("Property change result of {} node id {} prop id {} prop key [{}]: success={}",
                        change.getChangeType(), nodeId, propId, changeProperty.getPropKey(), success);
            }
            changeResults.add(new PropertyChangeResult(change, success, resultProperty));
        }
        properties.resetChangeTracking(changeResults);
    }

    private void repalceProperties(long nodeId, Properties properties) throws SQLException {
        log.trace("Replacing properties of node {}", nodeId);
        // first delete existing properties is exist
        deleteProperties(nodeId);

        // create record for each node property. one record for each key/value combination
        for (Map.Entry<String, String> propEntry : properties.entries()) {
            NodeProperty property = new NodeProperty(dbService.nextId(), nodeId, propEntry.getKey(),
                    propEntry.getValue());
            propertiesDao.create(property);
        }
    }

    /**
     * Sets properties
     *
     * @param repoPath   RepoPath of the node to set properties on
     * @param properties The properties to set
     */
    @Override
    public void setProperties(RepoPath repoPath, Properties properties) {
        long nodeId = fileService.getNodeId(repoPath);
        if (nodeId > 0) {
            setProperties(nodeId, properties);
        }
    }

    @Override
    public int deleteProperties(long nodeId) {
        try {
            return propertiesDao.deleteNodeProperties(nodeId);
        } catch (SQLException e) {
            throw new StorageException("Failed to delete properties for node: " + nodeId, e);
        }
    }
}

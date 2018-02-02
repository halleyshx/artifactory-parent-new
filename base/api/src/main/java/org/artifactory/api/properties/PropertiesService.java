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

package org.artifactory.api.properties;

import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yossi Shaul
 */
public interface PropertiesService {

    String FILTERED_RESOURCE_PROPERTY_NAME = "filtered";

    String MAVEN_PLUGIN_PROPERTY_NAME = "artifactory.maven.mavenPlugin";

    String CONTENT_TYPE_PROPERTY_NAME = "content-type";

    /**
     * @param repoPath The item (repository/folder/file) repository path
     * @return The properties attached to this repo path. Empty properties if non exist.
     */
    @Nonnull
    Properties getProperties(RepoPath repoPath);

    /**
     * Adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param property    Property to add
     * @param values      Property values (if null, will not add the property)
     */
    @Lock
    void addProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            boolean updateAccessLogger,String... values);

    @Lock
    void addProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,String... values);

    /**
     * Edit a property on a specific repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet The property set to edit
     * @param property    The property to add
     * @param values      Property values
     */
    @Lock
    void editProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property, boolean updateAccessLogger, String... values);

    /**
     * Recursively adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param property    Property to add
     * @param values      Property values (if null, will not add the property)
     */
    @Lock
    void addPropertyRecursively(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            boolean updateAccessLogger, String... values);

    /**
     * Recursively adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param property    Property to add
     * @param values      Property values (if null, will not add the property)
     */
    @Lock
    void addPropertyRecursively(RepoPath repoPath, @Nullable PropertySet propertySet, Property property, String... values);
    /**
     * Deletes the property from the item.
     *
     * @param repoPath The item repo path
     * @param property Property name to delete
     */
    @Lock
    boolean deleteProperty(RepoPath repoPath, String property,boolean updateAccessLogger);

    @Lock
    boolean deleteProperty(RepoPath repoPath, String property);

    /**
     * Recursively adds (and stores) a property to the item at the repo path in multiple transaction.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param propertyMapFromRequest    Properties map from request
     */
    void addPropertyRecursivelyMultiple(RepoPath repoPath, @Nullable PropertySet propertySet,
                                        Map<Property, List<String>> propertyMapFromRequest,boolean updateAccessLogger);

    @Deprecated //TODO [by dan][sha2]:remove after migration-enforcing major
    void addPropertySha256RecursivelyMultiple(RepoPath repoPath);

    /**
     * Deletes property from the item recursively.
     *
     * @param repoPath The item repo path
     * @param property Property name to delete
     */
    @Lock
    void deletePropertyRecursively(RepoPath repoPath, String property, boolean updateAccessLogger);

    /**
     * Deletes property from the item recursively.
     *
     * @param repoPath The item repo path
     * @param property Property name to delete
     */
    @Lock
    void deletePropertyRecursively(RepoPath repoPath, String property);

    /**
     * Returns map of properties for the given repo paths
     *
     * @param repoPaths     Paths to extract properties for
     * @param mandatoryKeys Any property keys that should be mandatory for resulting properties. If provided, property
     *                      objects will be added to the map only if they contain all the given keys
     * @return Map of repo paths with their corresponding properties
     */
    Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths, String... mandatoryKeys);

    /**
     * @return a mapping of nodeId -> {@link Properties} that are retrieved for all nodes under {@param repoKey}
     * that have property {@param propKey} with any values from {@param propValues}
     */
    Map<Long, Properties> getAllProperties(String repoKey, String propKey, List<String> propValues);

    @Lock
    void setProperties(RepoPath repoPath,Properties newProperties);
}
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

package org.artifactory.storage.db.fs.itest.dao;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.artifactory.storage.db.fs.dao.PropertiesDao;
import org.artifactory.storage.db.fs.entity.NodeProperty;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.jfrog.storage.DbType;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.*;

/**
 * Tests {@link org.artifactory.storage.db.fs.dao.PropertiesDao}.
 *
 * @author Yossi Shaul
 */
public class PropertiesDaoTest extends DbBaseTest {

    @Autowired
    private PropertiesDao propsDao;

    @BeforeClass
    public void setup() {
        importSql("/sql/nodes.sql");
    }

    public void hasPropertiesNodeWithProperties() throws SQLException {
        boolean result = propsDao.hasNodeProperties(5);
        assertTrue(result, "Node expected to hold properties");
    }

    public void hasPropertiesNodeWithoutProperties() throws SQLException {
        boolean result = propsDao.hasNodeProperties(1);
        assertFalse(result, "Node is not expected to hold properties");
    }

    public void hasPropertiesNodeNotExist() throws SQLException {
        boolean result = propsDao.hasNodeProperties(5478939);
        assertFalse(result, "Node that doesn't exist is not expected to hold properties");
    }

    public void getPropertiesNodeWithProperties() throws SQLException {
        List<NodeProperty> result = propsDao.getNodeProperties(5);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        for (NodeProperty property : result) {
            assertEquals(property.getNodeId(), 5, "All results should be with the same node id");
        }

        NodeProperty buildName = getById(1, result);
        assertEquals(buildName.getPropId(), 1);
        assertEquals(buildName.getPropKey(), "build.name");
        assertEquals(buildName.getPropValue(), "ant");
    }

    public void getPropertiesNodeWithEmptyProperties() throws SQLException {
        List<NodeProperty> result = propsDao.getNodeProperties(14);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        for (NodeProperty property : result) {
            assertEquals(property.getNodeId(), 14, "All results should be with the same node id");
        }

        NodeProperty emptyVal = getById(6, result);
        assertEquals(emptyVal.getPropId(), 6);
        assertEquals(emptyVal.getPropKey(), "empty.val");
        assertEquals(emptyVal.getPropValue(), "");

        NodeProperty nullVal = getById(7, result);
        assertEquals(nullVal.getPropId(), 7);
        assertEquals(nullVal.getPropKey(), "null.val");
        assertEquals(emptyVal.getPropValue(), "");
    }

    public void getPropertiesNodeWithoutProperties() throws SQLException {
        List<NodeProperty> result = propsDao.getNodeProperties(1);
        assertEquals(result.size(), 0);
    }

    public void getPropertiesNodeNotExist() throws SQLException {
        List<NodeProperty> result = propsDao.getNodeProperties(98958459);
        assertEquals(result.size(), 0);
    }

    public void insertProperty() throws SQLException {
        int createCount = propsDao.create(new NodeProperty(11, 9, "key1", "value1"));
        assertEquals(createCount, 1);
        createCount = propsDao.create(new NodeProperty(12, 9, "key2", "value2"));
        assertEquals(createCount, 1);

        List<NodeProperty> properties = propsDao.getNodeProperties(9);
        assertEquals(properties.size(), 2);
    }

    public void deletePropertiesNodeWithProperties() throws SQLException {
        // first check the properties exist
        assertEquals(propsDao.getNodeProperties(9).size(), 3);

        int deletedCount = propsDao.deleteNodeProperties(9);
        assertEquals(deletedCount, 3);
        assertEquals(propsDao.getNodeProperties(9).size(), 0);
    }

    public void deletePropertiesNodeWithNoProperties() throws SQLException {
        assertEquals(propsDao.deleteNodeProperties(1), 0);
    }

    public void deletePropertiesNonExistentNode() throws SQLException {
        assertEquals(propsDao.deleteNodeProperties(6778678), 0);
    }

    public void trimLongPropertyValue() throws SQLException {
        if (dbProperties.getDbType() == DbType.MSSQL) {
            return; // RTFACT-5768
        }
        String longValue = RandomStringUtils.randomAscii(4020);
        propsDao.create(new NodeProperty(876, 15, "trimeme", longValue));
        List<NodeProperty> nodeProperties = propsDao.getNodeProperties(15);
        assertThat(nodeProperties.size()).isEqualTo(1);
        String trimmedValue = nodeProperties.get(0).getPropValue();
        assertThat(trimmedValue).hasSize(4000);
        assertThat(longValue).startsWith(trimmedValue);
    }

    public void updatePropertyValue() throws SQLException {
        NodeProperty prop1 = new NodeProperty(1001, 37, "the-key1", "the-value1");
        assertEquals(propsDao.create(prop1), 1);
        NodeProperty prop2 = new NodeProperty(1002, 37, "the-key1", "the-value2");
        assertEquals(propsDao.create(prop2), 1);
        NodeProperty prop3 = new NodeProperty(1003, 37, "the-key2", "the-value3");
        assertEquals(propsDao.create(prop3), 1);
        //Update an existing property
        NodeProperty updatedProp1 = new NodeProperty(prop1.getPropId(), prop1.getNodeId(), prop1.getPropKey(), "the-new-value1");
        assertEquals(propsDao.updateValue(updatedProp1), 1);
        assertEqualProps(Arrays.asList(updatedProp1, prop2, prop3), propsDao.getNodeProperties(37));
        //Update a non-existing property
        NodeProperty updatedNonExistingProp = new NodeProperty(1004, 37, "the-key3", "the-value4");
        assertEquals(propsDao.updateValue(updatedNonExistingProp), 0, "Update should not change anything in this case");
        assertEqualProps(Arrays.asList(updatedProp1, prop2, prop3), propsDao.getNodeProperties(37));
        //Update an existing property but with mismatching key
        NodeProperty updatedPropWrongKey = new NodeProperty(prop2.getPropId(), prop2.getNodeId(), prop2.getPropKey()+"-wrong", "the-value5");
        assertEquals(propsDao.updateValue(updatedPropWrongKey), 0, "Update should not change anything in this case");
        assertEqualProps(Arrays.asList(updatedProp1, prop2, prop3), propsDao.getNodeProperties(37));
    }

    public void deleteProperty() throws SQLException {
        assertEquals(propsDao.getNodeProperties(36).size(), 0);
        NodeProperty prop1 = new NodeProperty(2001, 36, "the-key1", "the-value1");
        assertEquals(propsDao.create(prop1), 1);
        NodeProperty prop2 = new NodeProperty(2002, 36, "the-key1", "the-value2");
        assertEquals(propsDao.create(prop2), 1);
        NodeProperty prop3 = new NodeProperty(2003, 36, "the-key2", "the-value3");
        assertEquals(propsDao.create(prop3), 1);
        assertEqualProps(propsDao.getNodeProperties(36), Arrays.asList(prop1, prop2, prop3));

        propsDao.delete(prop1);
        assertEqualProps(propsDao.getNodeProperties(36), Arrays.asList(prop2, prop3));
        propsDao.delete(prop3);
        assertEqualProps(propsDao.getNodeProperties(36), Collections.singletonList(prop2));
        propsDao.delete(prop2);
        assertEqualProps(propsDao.getNodeProperties(36), Collections.emptyList());
    }

    private void assertEqualProps(List<NodeProperty> actual, List<NodeProperty> expected) {
        assertEquals(actual.size(), expected.size(), "Lists are not of the same size");
        List<NodeProperty> actualSorted = actual.stream()
                .sorted((p1, p2) -> Long.compare(p1.getPropId(), p2.getPropId()))
                .collect(Collectors.toList());
        List<NodeProperty> expectedSorted = expected.stream()
                .sorted((p1, p2) -> Long.compare(p1.getPropId(), p2.getPropId()))
                .collect(Collectors.toList());
        for (int i = 0; i < actualSorted.size(); i++) {
            NodeProperty actualProp = actualSorted.get(i);
            NodeProperty expectedProp = expectedSorted.get(i);
            if (!EqualsBuilder.reflectionEquals(actualProp, expectedProp)) {
                fail("props at index " + i + " are not the same.\nActual: " + toString(actualProp) + "\nExpected: " + toString(expectedProp));
            }
        }
    }

    private String toString(NodeProperty prop) {
        return "[" + prop.getNodeId() + ", " + prop.getPropId() +", [" + prop.getPropKey() + "], [" + prop.getPropValue() + "] ]";
    }

    private NodeProperty getById(long propId, List<NodeProperty> properties) {
        for (NodeProperty property : properties) {
            if (property.getPropId() == propId) {
                return property;
            }
        }
        return null;
    }
}

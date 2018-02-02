/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.storage.db.xray.dao;

import com.google.common.collect.Sets;
import org.artifactory.storage.db.fs.dao.NodesDao;
import org.artifactory.storage.db.fs.dao.PropertiesDao;
import org.artifactory.storage.db.fs.entity.NodeBuilder;
import org.artifactory.storage.db.fs.entity.NodeProperty;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertEquals;

/**
 * @author Yinon Avraham
 */
public class XrayDaoTest extends DbBaseTest {

    @Autowired
    private NodesDao nodesDao;

    @Autowired
    private PropertiesDao propsDao;

    @Autowired
    private XrayDao xrayDao;

    @BeforeClass
    public void setup() throws Exception {
        //repo1 nodes
        nodesDao.create(new NodeBuilder().nodeId(11).file(true).repo("repo1").path("path/to").name("file1.ext").build());
        nodesDao.create(new NodeBuilder().nodeId(12).file(true).repo("repo1").path("path/to").name("file2.ext1").build());
        nodesDao.create(new NodeBuilder().nodeId(13).file(true).repo("repo1").path("path/to/other").name("file3.ext1").build());
        nodesDao.create(new NodeBuilder().nodeId(14).file(true).repo("repo1").path("path/to/other").name("file4.ext2").build());
        nodesDao.create(new NodeBuilder().nodeId(15).file(true).repo("repo1").path("path/to/other").name("file5.ext2").build());
        nodesDao.create(new NodeBuilder().nodeId(16).file(true).repo("repo1").path("path/to/other").name("file6.ext2").build());
        nodesDao.create(new NodeBuilder().nodeId(17).file(true).repo("repo1").path("path/to/other").name("manifest.json").build());
        nodesDao.create(new NodeBuilder().nodeId(18).file(true).repo("repo1").path("path/to/another").name("file77.ext8").build());
        nodesDao.create(new NodeBuilder().nodeId(19).file(true).repo("repo1").path("path/to/another").name("file88.ext7").build());

        //repo2 nodes
        nodesDao.create(new NodeBuilder().nodeId(21).file(true).repo("repo2").path("path/to/mother").name("file1.ext1").build());
        nodesDao.create(new NodeBuilder().nodeId(22).file(true).repo("repo2").path("path/to/earth").name("file2.ext2").build());
        nodesDao.create(new NodeBuilder().nodeId(23).file(true).repo("repo2").path("path/to/earth").name("file3.ext2").build());
        nodesDao.create(new NodeBuilder().nodeId(24).file(true).repo("repo2").path("path/to/earth2").name("file3.ext3").build());
        nodesDao.create(new NodeBuilder().nodeId(25).file(true).repo("repo2").path("path/to/earth2").name("file4.ext3").build());
        nodesDao.create(new NodeBuilder().nodeId(26).file(true).repo("repo2").path("path/to/earth2").name("file5.ext3").build());

        long propId = 1;
        //repo1 props
        propsDao.create(new NodeProperty(propId++, 11, "xray.12345.index.status", "Indexed"));
        propsDao.create(new NodeProperty(propId++, 11, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 12, "xray.12345.index.status", "Indexing"));
        propsDao.create(new NodeProperty(propId++, 12, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 13, "xray.12345.index.status", "Indexed"));
        propsDao.create(new NodeProperty(propId++, 13, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 14, "xray.12345.index.status", "Indexed"));
        propsDao.create(new NodeProperty(propId++, 14, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));

        propsDao.create(new NodeProperty(propId++, 18, "xray.12345.index.status", "Scanned"));
        propsDao.create(new NodeProperty(propId++, 18, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 19, "xray.12345.index.status", "Scanned"));
        propsDao.create(new NodeProperty(propId++, 19, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));

        //repo2 props
        propsDao.create(new NodeProperty(propId++, 21, "xray.12345.index.status", "Indexed"));
        propsDao.create(new NodeProperty(propId++, 21, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 22, "xray.12345.index.status", "Indexing"));
        propsDao.create(new NodeProperty(propId++, 22, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 23, "xray.12345.index.status", "Indexing"));
        propsDao.create(new NodeProperty(propId++, 23, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 24, "xray.12345.index.status", "Indexing"));
        propsDao.create(new NodeProperty(propId++, 24, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
        propsDao.create(new NodeProperty(propId++, 25, "xray.12345.index.status", "Indexed"));
        propsDao.create(new NodeProperty(propId++, 25, "xray.12345.index.lastUpdated", "" + System.currentTimeMillis()));
    }

    @Test
    public void testCountArtifactsByIndexStatus() {
        assertEquals(xrayDao.countArtifactsByIndexStatus("repo1", "xray.12345.index.status", "Indexed"), 3);
        assertEquals(xrayDao.countArtifactsByIndexStatus("repo1", "xray.12345.index.status", "Indexing"), 1);
        assertEquals(xrayDao.countArtifactsByIndexStatus("repo1", "xray.12345.index.status", "Scanned"), 2);
        assertEquals(xrayDao.countArtifactsByIndexStatus("repo2", "xray.12345.index.status", "Indexed"), 2);
        assertEquals(xrayDao.countArtifactsByIndexStatus("repo2", "xray.12345.index.status", "Indexing"), 3);
    }

    @Test
    public void testGetPotentialForIndex() {
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext"), Collections.emptySet()), 1);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext1"), Collections.emptySet()), 2);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext2"), Collections.emptySet()), 3);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext", "ext2"), Collections.emptySet()), 4);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext1", "ext2"), Collections.emptySet()), 5);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext", "ext1", "ext2"), Collections.emptySet()), 6);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("non"), Collections.emptySet()), 0);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("non", "bon"), Collections.emptySet()), 0);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("non", "bon"), Sets.newHashSet("manifest.json")), 1);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext1", "ext2"), Sets.newHashSet("manifest.json")), 6);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext", "ext1"), Sets.newHashSet("manifest.json")), 4);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Sets.newHashSet("ext", "ext1"), Sets.newHashSet("manifest.jsonXYZ")), 3);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Collections.emptySet(), Sets.newHashSet("manifest.json")), 1);
        assertEquals(xrayDao.getPotentialForIndex("repo1", Collections.emptySet(), Sets.newHashSet("manifest.jsonXYZ")), 0);

        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext1"), Collections.emptySet()), 1);
        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext2"), Collections.emptySet()), 2);
        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext3"), Collections.emptySet()), 3);
        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext1", "ext2"), Collections.emptySet()), 3);
        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext2", "ext3"), Collections.emptySet()), 5);
        assertEquals(xrayDao.getPotentialForIndex("repo2", Sets.newHashSet("ext1", "ext2", "ext3"), Collections.emptySet()), 6);
    }
}
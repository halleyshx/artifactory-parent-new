package org.artifactory.storage.db.build.service;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BuildPropertyAsKeyValueTest {
    @Test
    public void testEquals() throws Exception {
        BuildPropertyAsKeyValue kv1 = new BuildPropertyAsKeyValue("a", "b");
        BuildPropertyAsKeyValue kv2 = new BuildPropertyAsKeyValue("a", "b");
        BuildPropertyAsKeyValue kv3 = new BuildPropertyAsKeyValue("b", "a");
        BuildPropertyAsKeyValue kv4 = new BuildPropertyAsKeyValue("c", "b");
        BuildPropertyAsKeyValue kv5 = new BuildPropertyAsKeyValue("a", "c");
        assertEquals(kv1, kv1);
        assertEquals(kv1, kv2);
        assertNotEquals(kv1, kv3);
        assertNotEquals(kv1, kv4);
        assertNotEquals(kv1, kv5);
    }

    @Test
    public void testHashCode() throws Exception {
        BuildPropertyAsKeyValue kv1 = new BuildPropertyAsKeyValue("a", "b");
        BuildPropertyAsKeyValue kv2 = new BuildPropertyAsKeyValue("a", "b");
        BuildPropertyAsKeyValue kv3 = new BuildPropertyAsKeyValue("b", "a");
        BuildPropertyAsKeyValue kv4 = new BuildPropertyAsKeyValue("c", "b");
        BuildPropertyAsKeyValue kv5 = new BuildPropertyAsKeyValue("a", "c");
        assertEquals(kv1.hashCode(), kv1.hashCode());
        assertEquals(kv1.hashCode(), kv2.hashCode());
        assertNotEquals(kv1.hashCode(), kv3.hashCode());
        assertNotEquals(kv1.hashCode(), kv4.hashCode());
        assertNotEquals(kv1.hashCode(), kv5.hashCode());
    }

}
package org.artifactory.util.encoding;

import org.artifactory.util.IdUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Noam Shemesh
 */
public class IdUtilsTest {
    @Test
    public void testProduceIdWithEmptyUrl() throws Exception {
        String res = IdUtils.produceReplicationId("abc", null);
        assertEquals(res, "abc_");
    }

    @Test
    public void testProduceIdWithLongUrl() throws Exception {
        String res = IdUtils.produceReplicationId("abc",
                "http://example.com/hello/world/goingtobeshortenedandreplacedwithhash");

        assertEquals(res, "abc_http___example_c95e299139f");
    }

    @Test
    public void testProduceIdWithEmptyRepoKey() throws Exception {
        String res = IdUtils.produceReplicationId(null, "http://example");
        assertEquals(res, "_http___example");
    }

    @Test
    public void testProduceIdWithMediumSizeConcat() throws Exception {
        String res = IdUtils.produceReplicationId("0123456789",
                "http://example.com");

        assertNotEquals(res, "0123456789_http___example_com");
        assertTrue(res.startsWith("0123456789_http___ex"));
    }
}
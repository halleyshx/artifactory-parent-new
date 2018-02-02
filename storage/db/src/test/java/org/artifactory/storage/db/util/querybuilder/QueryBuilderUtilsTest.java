package org.artifactory.storage.db.util.querybuilder;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.artifactory.storage.db.util.querybuilder.QueryBuilderUtils.addButLimit;
import static org.testng.Assert.*;

/**
 * @author Yinon Avraham
 * Created on 09/08/2016.
 */
@Test
public class QueryBuilderUtilsTest {

    @Test(dataProvider = "provideAddButLimit")
    public void testAddButLimit(long a, long b, long max, long expected) {
        assertEquals(addButLimit(a, b, max), expected);
    }

    @DataProvider
    public static Object[][] provideAddButLimit() {
        return new Long[][] {
                {0L,0L,0L,0L},
                {0L,5L,10L,5L},
                {5L,0L,10L,5L},
                {300L,200L,1000L,500L},
                {0L,10L,5L,5L},
                {10L,0L,5L,5L},
                {Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE},
                {Long.MAX_VALUE-10,Long.MAX_VALUE-10,Long.MAX_VALUE,Long.MAX_VALUE},
                {Long.MAX_VALUE,Long.MAX_VALUE,10L,10L},
        };
    }

    @Test(dataProvider = "provideAddButLimitIllegalArgs", expectedExceptions = {AssertionError.class})
    public void testAddButLimitIllegalArgs(long a, long b, long max) {
        addButLimit(a, b, max);
    }

    @DataProvider
    public static Object[][] provideAddButLimitIllegalArgs() {
        return new Long[][] {
                {-1L,0L,0L},
                {0L,-2L,0L},
                {0L,0L,-3L},
                {Long.MIN_VALUE,0L,0L},
                {0L,Long.MIN_VALUE,0L},
                {0L,0L,Long.MIN_VALUE}
        };
    }

}
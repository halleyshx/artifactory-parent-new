/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Yinon Avraham
 */
@Test
public class InternalStringUtilsTest {

    @Test(dataProvider = "provideCompareNullLast")
    public void testCompareNullLast(String s1, String s2, int expected) {
        assertEquals(Math.signum(InternalStringUtils.compareNullLast(s1, s2)), Math.signum(expected));
    }

    @DataProvider
    private Object[][] provideCompareNullLast() {
        return new Object[][] {
                { "abc", "xyz", -1 },
                { "abc", "abc", 0 },
                { "xyz", "abc", 1 },
                { "abc", null, 1 },
                { null, "abc", -1 },
                { null, null, 0 },
        };
    }

}
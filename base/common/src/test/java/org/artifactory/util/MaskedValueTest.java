/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.util;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * <p>Created on 18/07/16
 *
 * @author Yinon Avraham
 */
public class MaskedValueTest {

    @Test
    public void testMaskedValue() {
        assertEquals(MaskedValue.of(null).toString(), "null");
        assertEquals(MaskedValue.of("1234567890abcde").toString(), "*******cde");
        assertEquals(MaskedValue.of("1234567890abcd").toString(), "*******bcd");
        assertEquals(MaskedValue.of("1234567890abc").toString(), "*******abc");
        assertEquals(MaskedValue.of("1234567890ab").toString(), "*******0ab");
        assertEquals(MaskedValue.of("1234567890a").toString(), "*******90a");
        assertEquals(MaskedValue.of("1234567890").toString(), "*******890");
        assertEquals(MaskedValue.of("123456789").toString(), "**********");
        assertEquals(MaskedValue.of("12345678").toString(), "**********");
        assertEquals(MaskedValue.of("1234567").toString(), "**********");
        assertEquals(MaskedValue.of("123456").toString(), "**********");
        assertEquals(MaskedValue.of("12345").toString(), "**********");
        assertEquals(MaskedValue.of("1234").toString(), "**********");
        assertEquals(MaskedValue.of("123").toString(), "**********");
        assertEquals(MaskedValue.of("12").toString(), "**********");
        assertEquals(MaskedValue.of("1").toString(), "**********");
        assertEquals(MaskedValue.of("").toString(), "**********");
        assertEquals(MaskedValue.of(Long.MAX_VALUE).toString(), "*******807");
        assertEquals(MaskedValue.of(Integer.MAX_VALUE).toString(), "*******647");
    }

}
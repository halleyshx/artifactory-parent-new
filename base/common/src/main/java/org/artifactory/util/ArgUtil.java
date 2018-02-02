/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.util;

import java.util.function.Predicate;

/**
 * Utilities for handling method arguments
 * <p>Created on 19/07/16
 *
 * @author Yinon Avraham
 */
public final class ArgUtil {

    private ArgUtil() {}

    /**
     * Check the value of the given argument is accepted by the predicate.
     * @param value     the value to check
     * @param predicate the predicate to use
     * @param message   the error message for the exception
     * @param <T>       the type of the value
     * @throws IllegalArgumentException If the check fails
     */
    public static <T> T checkValue(T value, Predicate<T> predicate, String message) {
        if (!predicate.test(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}

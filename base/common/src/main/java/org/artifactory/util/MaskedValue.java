/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Value wrapper that masks the string value (produced by applying {@link String#valueOf(Object)} on the given object).
 * For example, the masked value of <code>"1234567890abcdef"</code> is <code>"*******def"</code>.
 * The goal is to hide the full text representation but still give some indication on the actual value.
 * <p>Created on 18/07/16
 *
 * @author Yinon Avraham
 */
public class MaskedValue {
    private final Object obj;

    private MaskedValue(Object obj) {
        this.obj = obj;
    }

    public static MaskedValue of(@Nullable Object obj) {
        return new MaskedValue(obj);
    }

    @Override
    public String toString() {
        return obj == null ? "null" : masked(String.valueOf(obj));
    }

    private String masked(@Nonnull String value) {
        if (value.length() < 10) {
            return "**********";
        }
        return "*******" + value.substring(value.length()-3);
    }
}

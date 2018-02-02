/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.security;

/**
 * <p>Created on 30/06/16
 *
 * @author Yinon Avraham
 */
public class ResetPasswordException extends RuntimeException {

    public ResetPasswordException(String message) {
        super(message);
    }

    public static ResetPasswordException tooManyAttempts(String remoteAddress) {
        return new ResetPasswordException("Too many reset password requests, retry again at a later time.");
    }

    public static ResetPasswordException tooFrequentAttempts(String remoteAddress) {
        return new ResetPasswordException("Too frequent reset password requests, retry again shortly.");
    }
}

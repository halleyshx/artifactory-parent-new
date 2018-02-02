/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.sumologic;

/**
 * @author Shay Yaakov
 */
public class SumoLogicException extends RuntimeException {

    private final int status;

    public SumoLogicException(String message) {
        super(message);
        status = 500;
    }

    public SumoLogicException(String message, int status) {
        super(message);
        this.status = status;
    }

    public SumoLogicException(String message, Throwable cause) {
        super(message, cause);
        status = 500;
    }

    public int getStatus() {
        return status;
    }

    public int getRelaxedStatus() {
        if (status == 401 || status == 403) {
            //Artifactory UI has a default behavior for unauthorized and forbidden. To avoid it we change to bad request.
            return 400;
        } else if (status == 503) {
            //Artifactory UI has a default behavior for service unavailable. To avoid it we change to internal server error.
            return 500;
        }
        return status;
    }
}
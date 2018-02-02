/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.common.exception;

import org.artifactory.exception.CancelException;
import org.artifactory.rest.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Intercepts CancelException and return JSON error response. All of the api packages downloads that ran into a
 * CancelException through a user plugin will be mapped to a JSON error using this mapper, See RTFACT-8257.
 *
 * @author Shay Bagants
 */
@Component
@Provider
public class CancelExceptionMapper implements ExceptionMapper<CancelException> {
    private static final Logger log = LoggerFactory.getLogger(CancelExceptionMapper.class);

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(CancelException e) {
        String requestPath = new UrlPathHelper().getPathWithinApplication(request);
        String message = buildResponseMessage(requestPath, e);
        writeToLog(message, e);
        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), message);
        return Response.status(e.getErrorCode()).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }

    private String buildResponseMessage(String requestPath, CancelException e) {
        String message;
        //basically, expect api's downloads (nuget, npm etc..) and the plugin execute, the other paths are already handles the exceptions,
        // but I've added these anyway to handle possible future change
        if (requestPath.startsWith("/api/plugins/execute/") || requestPath.startsWith("/api/plugins/build/promote/") ||
                requestPath.startsWith("/api/storage/") || requestPath.startsWith("/api/copy/") || requestPath.startsWith("/api/move/")) {
            message = "Request has been canceled";
        } else {
            message = "Download request has been canceled";
        }
        return message + (e.getMessage() != null ? ": " + e.getMessage() : ".");
    }

    private void writeToLog(String message, CancelException e) {
        log.info(message);
        if (log.isDebugEnabled()) {
            log.debug(message, e);
        }
    }
}

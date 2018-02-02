/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.rest.util;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.common.StatusEntry;
import org.codehaus.jackson.JsonGenerator;

import javax.ws.rs.core.StreamingOutput;

/**
 * Utility class that helps creating responses
 *
 * @author Shay Bagants
 */
public class ResponseUtils {

    public static StreamingOutput getStreamingOutput(BasicStatusHolder result) {
        return outputStream -> {
            JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("messages");
            if (result.hasErrors()) {
                for (StatusEntry error : result.getErrors()) {
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField("level", error.getLevel().name());
                    jsonGenerator.writeStringField("message", error.getMessage());
                    jsonGenerator.writeEndObject();
                }
            }
            if (result.hasWarnings()) {
                for (StatusEntry warning : result.getWarnings()) {
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField("level", warning.getLevel().name());
                    jsonGenerator.writeStringField("message", warning.getMessage());
                    jsonGenerator.writeEndObject();
                }
            }
            //No errors \ warnings - return last info
            if (!result.hasErrors() && !result.hasWarnings()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("level", result.getLastStatusEntry().getLevel().name());
                jsonGenerator.writeStringField("message", result.getLastStatusEntry().getMessage());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
        };
    }

    public static StreamingOutput getResponseWithStatusCodeErrorAndErrorMassages(BasicStatusHolder result, String message, int statusCode){
        return outputStream -> {
            JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
            jsonGenerator.writeStartObject();
            if (result.hasErrors() || result.hasWarnings()) {
                jsonGenerator.writeArrayFieldStart("errors");
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("status", statusCode);
                jsonGenerator.writeStringField("message", message);
                jsonGenerator.writeEndObject();
                jsonGenerator.writeEndArray();

                jsonGenerator.writeArrayFieldStart("error massages");
                for (StatusEntry error : result.getErrors()) {
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField("level", error.getLevel().name());
                    jsonGenerator.writeStringField("message", error.getMessage());
                    jsonGenerator.writeEndObject();
                }
                jsonGenerator.writeEndArray();
                jsonGenerator.writeEndObject();
                jsonGenerator.close();
            }
        };
    }
}

package org.artifactory.storage.db.aql.service;

import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.aql.result.AqlJsonStreamer;
import org.artifactory.aql.result.AqlLazyResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Yinon Avraham
 * Created on 09/08/2016.
 */
public final class AqlTestUtils {

    private AqlTestUtils() {}

    public static Map parseQueryResult(AqlLazyResult queryResult) throws IOException {
        Map parsedResult;
        try (AqlJsonStreamer streamer = new AqlJsonStreamer(queryResult)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes;
            while ((bytes = streamer.read()) != null) {
                out.write(bytes);
            }
            parsedResult = JacksonReader.bytesAsClass(out.toByteArray(), Map.class);
        }
        return parsedResult;
    }

}

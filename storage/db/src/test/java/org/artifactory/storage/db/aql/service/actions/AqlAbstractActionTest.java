package org.artifactory.storage.db.aql.service.actions;

import org.artifactory.aql.result.AqlJsonStreamer;
import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.storage.db.aql.service.AqlAbstractServiceTest;

/**
 * @author Dan Feldman
 */
public class AqlAbstractActionTest extends AqlAbstractServiceTest {

    String executeQuery(String query) throws Exception {
        AqlLazyResult queryResult = null;
        StringBuilder builder = new StringBuilder();
        try {
            queryResult = aqlService.executeQueryLazy(query);
            AqlJsonStreamer jsonStreamer = new AqlJsonStreamer(queryResult);
            byte[] read;
            while ((read = jsonStreamer.read()) != null) {
                builder.append(new String(read));
            }
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }
        return builder.toString();
    }

}

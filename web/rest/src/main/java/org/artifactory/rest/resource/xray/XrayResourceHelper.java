package org.artifactory.rest.resource.xray;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.artifactory.addon.xray.XrayScanBuild;
import org.artifactory.rest.common.model.xray.XrayScanBuildModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;

/**
 * @author Chen Keinan
 */
class XrayResourceHelper {

    /**
     * map rest model to service model
     * @param xrayScanBuildModel - rest api model (payload)
     * @return xray service model to be send to xray
     */
    static XrayScanBuild toModel(XrayScanBuildModel xrayScanBuildModel){
        return new XrayScanBuild(xrayScanBuildModel.getBuildName(),
                xrayScanBuildModel.getBuildNumber(),
                xrayScanBuildModel.getContext());
    }

    /**
     * Stream response from xray
     * @param inputStream - input stream from xray response
     * @return stream response data from xray to client
     */
    static Response streamResponse(InputStream inputStream){
          /* stream build scanning results */
        StreamingOutput stream = out -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(inputStream);
            }
        };
        return Response.status(HttpStatus.SC_OK).entity(stream).build();
    }
}

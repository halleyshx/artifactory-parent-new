package org.artifactory.addon.replication.event;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author nadavy
 */
public interface OutboundReplicationChannel extends ReplicationChannel {

    /**
     * Used by the {@link javax.ws.rs.core.StreamingOutput} that's returned to the replicating end.
     * This stream is being read by {@link org.artifactory.addon.replication.core.remote.event.InboundReplicationChannel.IncomingStreamReader}
     */
    void write(OutputStream outputStream);

    String getNextEventAsJson();

    void putEvent(ReplicationEventQueueWorkItem eventQueueItem) throws IOException;

    void destroy();

    String getUuid();
}

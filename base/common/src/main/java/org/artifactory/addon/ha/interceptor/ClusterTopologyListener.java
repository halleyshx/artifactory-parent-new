package org.artifactory.addon.ha.interceptor;

import org.artifactory.interceptor.Interceptor;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;

import java.util.List;

/**
 * Notifies interested parties about changes to the cluster topology
 *
 * @author Gidi Shabat
 * @author Dan Feldman
 */
public interface ClusterTopologyListener extends Interceptor {

    /**
     * Passes the list of currently active, properly licensed {@param activeNodes} to interested listeners.
     */
    void clusterTopologyChanged(List<ArtifactoryServer> activeNodes);
}

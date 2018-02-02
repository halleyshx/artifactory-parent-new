package org.artifactory.addon.ha;

import org.artifactory.addon.ha.interceptor.ClusterTopologyListener;
import org.artifactory.spring.ReloadableBean;

/**
 * Commonly available interface for cluster actions that needs to performed from the OSS module.
 *
 * @author Dan Feldman
 */
public interface ClusterOperationsService extends ReloadableBean, ClusterTopologyListener {

}

package org.artifactory.state.model;

import org.artifactory.spring.ReloadableBean;

/**
 * @author Shay Bagants
 */
//TODO [by shayb]: Dummy implementation on the oss + add priority
public interface StateInitializer extends ReloadableBean {

    String getSupportBundleDump();

}

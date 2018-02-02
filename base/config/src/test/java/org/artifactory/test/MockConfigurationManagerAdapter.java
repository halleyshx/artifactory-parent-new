package org.artifactory.test;

import org.artifactory.common.ArtifactoryConfigurationAdapter;
import org.jfrog.config.BroadcastChannel;
import org.jfrog.config.DbChannel;
import org.jfrog.config.Home;
import org.jfrog.config.LogChannel;

/**
 * @author gidis
 */
public class MockConfigurationManagerAdapter extends ArtifactoryConfigurationAdapter {

    public MockConfigurationManagerAdapter(Home home) {
        super(home);
    }

    @Override
    public void initialize() {
        this.home.initArtifactorySystemProperties();
        this.primary = home.getArtifactoryHaNodePropertiesFile().exists() && home.getHaNodeProperties() != null
                && home.getHaNodeProperties().isPrimary();
        this.ha = home.getArtifactoryHaNodePropertiesFile().exists();
    }

    @Override
    public LogChannel getLogChannel() {
        return null;
    }

    @Override
    public DbChannel getDbChannel() {
        return null;
    }

    @Override
    public BroadcastChannel getBroadcastChannel() {
        return null;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void setPermanentLogChannel() {
    }

    @Override
    public void setPermanentBroadcastChannel(BroadcastChannel broadcastChannel) {
    }

    @Override
    public void setPermanentDBChannel(DbChannel permanentDbChannel) {
    }
}

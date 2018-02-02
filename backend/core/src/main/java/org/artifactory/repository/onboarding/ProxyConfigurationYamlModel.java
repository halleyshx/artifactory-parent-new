package org.artifactory.repository.onboarding;

/**
 * Model of proxy yaml configuration loaded from artifactory.config.yaml
 * Part of org.artifactory.repository.onboarding.ConfigurationYamlModel
 *
 * @author nadavy
 */
public class ProxyConfigurationYamlModel {
    public String key;
    public String host;
    public int port;
    public String userName;
    public String password;
    public boolean defaultProxy;
}

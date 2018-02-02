package org.artifactory.repository.onboarding;

/**
 * Model of yaml configuration loaded from artifactory.config.yaml
 *
 * @author nadavy
 */
public class ConfigurationYamlModel {

    public String version;
    public GeneralConfigurationYamlModel GeneralConfiguration;
    public OnboardingConfigurationYamlModel OnboardingConfiguration;

}

package org.artifactory.repository.onboarding;

import java.util.List;
import java.util.Set;

/**
 * @author nadavy
 */
public class GeneralConfigurationYamlModel {
    public String licenseKey;
    public Set<String> licenseKeys;
    public String baseUrl;
    public List<ProxyConfigurationYamlModel> proxies;

}

package org.artifactory.repo.onboarding;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates a bootstrap yaml file using given repo types list and a config descriptor
 *
 * @author nadavy
 */
public class YamlConfigCreator {
    private static final Logger log = LoggerFactory.getLogger(YamlConfigCreator.class);

    private static final String BASE_URL_PLACEHOLDER = "{baseurl-placeholder}";
    private static final String PROXY_PLACEHOLDER = "{proxies-placeholder}";
    private static final String REPOSITORY_PLACEHOLDER = "{repotypes-placeholder}";
    private final static String YAML_TEMPLATE_FILE = "/templates/artifactory.config.template.yml";
    private final static String EMPTY_PROXY = "#proxies :\n" +
            "  # -  key : \"proxy1\"\n" +
            "  #    host : \"https://proxy.mycomp.io\"\n" +
            "  #    port : 443\n" +
            "  #    userName : \"admin\"\n" +
            "  #    password : \"password\"\n" +
            "  #    defaultProxy : true\n" +
            "  # -  key : \"proxy2\"\n" +
            "  #    ...\n";
    private final static String EMPTY_BASE_URL = "  #baseUrl : \"https://mycomp.arti.co\"\n";

    private MutableCentralConfigDescriptor configDescriptor;
    private List<String> repoTypeList;

    public void saveBootstrapYaml(List<String> repoTypeList, MutableCentralConfigDescriptor configDescriptor,
            File yamlOutputFile) throws IOException {
        this.configDescriptor = configDescriptor;
        this.repoTypeList = repoTypeList;
        try (InputStream inputStream = getClass().getResourceAsStream(YAML_TEMPLATE_FILE)) {
            List<String> yamlStringList = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
            String yamlOutputString = yamlStringList.stream()
                    .map(this::handleYamlLine)
                    .collect(Collectors.joining());
            log.info("Saving bootstrap settings to " + yamlOutputFile.getName());
            FileUtils.writeStringToFile(yamlOutputFile, yamlOutputString);
        }
    }

    private String handleYamlLine(String line) {
        if (line.contains(BASE_URL_PLACEHOLDER)) {
            return getYamlBaseUrl();
        }
        if (line.contains(PROXY_PLACEHOLDER)) {
            return getYamlProxy();
        }
        if (line.contains(REPOSITORY_PLACEHOLDER)) {
            return getYamlRepositories();
        }
        return line + "\n";
    }

    private String getYamlBaseUrl() {
        String baseUrl = configDescriptor.getUrlBase();
        if (Strings.isNullOrEmpty(baseUrl)) {
            baseUrl = EMPTY_BASE_URL;
        } else {
            baseUrl = "  baseUrl : \"" + baseUrl + "\"\n";
        }
        return baseUrl;
    }

    private String getYamlProxy() {
        List<ProxyDescriptor> proxiesDescriptors = configDescriptor.getProxies();
        String proxies = "";
        if (proxiesDescriptors == null) {
            proxies = EMPTY_PROXY;
        } else {
            proxies += "  proxies : \n";
            for (ProxyDescriptor proxyDescriptor : proxiesDescriptors) {
                proxies +=
                        "  - key : \"" + proxyDescriptor.getKey() + "\"\n" +
                                "    host : \"" + proxyDescriptor.getHost() + "\"\n" +
                                "    port : " + proxyDescriptor.getPort() + "\n" +
                                createPropertyIfNotNullOrEmpty("userName", proxyDescriptor.getUsername()) +
                                createPropertyIfNotNullOrEmpty("password", proxyDescriptor.getPassword()) +
                                "    defaultProxy : " + proxyDescriptor.isDefaultProxy() + "\n";
            }
        }
        return proxies;
    }

    private String createPropertyIfNotNullOrEmpty(String property, String value) {
        return Strings.isNullOrEmpty(value) ? "" : "    " + property + " : \"" + value + "\"\n";
    }

    private String getYamlRepositories() {
        String repos = "";
        for (String repoType : repoTypeList) {
            repos += "  - " + repoType + "\n";
        }
        return repos;
    }

}

package org.artifactory.addon.helm;

import lombok.Data;

import java.util.List;

/**
 * @author nadavy
 */
@Data
public class HelmInfo {

    private String name;
    private String version;
    private String appVersion;
    private String created;
    private String description;
    private List<String> keywords;
    private List<String> maintainers;
    private List<String> sources;
    private Boolean deprecated;

    HelmInfo(String name, String version, String appVersion, String created, String description,
            List<String> keywords, List<String> maintainers, List<String> sources, Boolean deprecated) {
        this.name = name;
        this.version = version;
        this.appVersion = appVersion;
        this.created = created;
        this.description = description;
        this.keywords = keywords;
        this.maintainers = maintainers;
        this.sources = sources;
        this.deprecated = deprecated;
    }
}


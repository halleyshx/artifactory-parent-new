package org.artifactory.addon.chef;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 *
 * Chef Cookbook metadata holder.
 *
 * @author Alexis Tual
 */
public class ChefCookbookInfo {

    private String name;
    private String version;
    private String description;
    private String maintainer;
    private String sourceUrl;
    private String license;

    private Map<String, String> dependencies = Maps.newHashMap();
    private Map<String, String> platforms = Maps.newHashMap();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Map<String, String> platforms) {
        this.platforms = platforms;
    }
}

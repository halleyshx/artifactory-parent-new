package org.artifactory.addon.composer;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

/**
 * @author Shay Bagants
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ComposerInfo {

    private String name;
    private String version;
    private String type;
    private List<String> keywords;
    private List<String> licenses;
    private List<String> authors;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }
}

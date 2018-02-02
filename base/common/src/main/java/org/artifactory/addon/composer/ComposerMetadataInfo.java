package org.artifactory.addon.composer;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

/**
 * @author Shay Bagants
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ComposerMetadataInfo {

    @JsonProperty("composerGeneralInfo")
    private ComposerInfo composerInfo;
    @JsonProperty("composerDependencies")
    private List<ComposerDependency> dependencies;
    private String description;

    public ComposerInfo getComposerInfo() {
        return composerInfo;
    }

    public void setComposerInfo(ComposerInfo composerInfo) {
        this.composerInfo = composerInfo;
    }

    public List<ComposerDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ComposerDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

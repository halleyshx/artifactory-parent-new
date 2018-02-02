package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.helm;

/**
 * @author nadavy
 */
public class HelmDescription {

    private String description;

    public HelmDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.puppet;

/**
 * @author Yinon Avraham.
 */
public class PuppetDependencyInfoModel {

    private String name;
    private String version;

    //for serialization
    public PuppetDependencyInfoModel() { }

    public PuppetDependencyInfoModel(String name, String version) {
        this.name = name;
        this.version = version;
    }

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
}

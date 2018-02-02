package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.puppet;

/**
 * @author Yinon Avraham.
 */
public class PuppetKeywordInfoModel {

    private String name;

    //for serialization
    public PuppetKeywordInfoModel() { }

    public PuppetKeywordInfoModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

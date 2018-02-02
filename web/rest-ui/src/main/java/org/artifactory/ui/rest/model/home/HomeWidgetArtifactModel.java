package org.artifactory.ui.rest.model.home;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Dan Feldman
 */
public class HomeWidgetArtifactModel extends BaseModel {

    private String path;
    private String downloadLink;
    private long downloads;

    public HomeWidgetArtifactModel(String path, String downloadLink, long downloads) {
        this.path = path;
        this.downloadLink = downloadLink;
        this.downloads = downloads;
    }

    public String getPath() {
        return path;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public long getDownloads() {
        return downloads;
    }
}

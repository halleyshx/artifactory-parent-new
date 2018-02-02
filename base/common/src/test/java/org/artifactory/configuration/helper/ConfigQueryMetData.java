package org.artifactory.configuration.helper;

/**
 * @author gidis
 */
public class ConfigQueryMetData {
    private String blob;
    private String path;

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBlob() {
        return blob;
    }

    public String getPath() {
        return path;
    }
}

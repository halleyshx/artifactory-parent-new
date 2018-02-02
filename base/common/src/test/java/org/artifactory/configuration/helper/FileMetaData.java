package org.artifactory.configuration.helper;

/**
 * @author gidis
 */
public class FileMetaData {
    private String path;
    private String content;

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

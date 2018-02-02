package org.artifactory.download;

/**
 * @author Dan Feldman
 */
public class FolderDownloadException extends RuntimeException {

    private final int code;

    public FolderDownloadException(String msg, int code) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

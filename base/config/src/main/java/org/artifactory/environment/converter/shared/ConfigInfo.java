package org.artifactory.environment.converter.shared;

/**
 * @author Shay Bagants
 */
public class ConfigInfo {

    public final String name;
    public final byte[] data;

    public ConfigInfo(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }
}

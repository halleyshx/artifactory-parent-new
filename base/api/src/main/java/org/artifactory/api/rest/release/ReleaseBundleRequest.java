package org.artifactory.api.rest.release;

/**
 * Model represent a release bundle request
 *
 * @author Shay Bagants
 */
public class ReleaseBundleRequest {
    private String uuid;
    private String signature;
    private String aql;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAql() {
        return aql;
    }

    public void setAql(String aql) {
        this.aql = aql;
    }
}

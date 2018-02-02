package org.artifactory.rest.resource.system;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shay Bagants
 */
public class LicenseRestResponse {

    private int status = 200;
    private Map<String, String> messages = new HashMap<>();

    public LicenseRestResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    @JsonIgnore
    public void addMessage(String licenseKey, String status) {
        messages.put(licenseKey, status);
    }
}

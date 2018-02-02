package org.artifactory.addon.oauth;

/**
 * A generic model for login request of Sso
 *
 * @author Yuval Reches
 */
public class SsoLoginModel {

    private String name;
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

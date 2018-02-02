package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission;

import java.util.List;
import java.util.Set;

/**
 * @author nadavy
 */
public class PermissionTargetInfo {

    private String permissionName;
    private List<String> repoKeys;
    private Set<String> groups;
    private Set<String> users;

    public PermissionTargetInfo() {
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }

    public void setRepoKeys(List<String> repoKeys) {
        this.repoKeys = repoKeys;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
}
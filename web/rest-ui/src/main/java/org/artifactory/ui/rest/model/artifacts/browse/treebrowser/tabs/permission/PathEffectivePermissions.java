/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission;

import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
public class PathEffectivePermissions extends BaseArtifactInfo {

    @JsonIgnore
    private static final int permissionTargetCap = 5;
    private String principal;
    private boolean admin;
    private List<String> permissionTargets;
    private EffectivePermission permission;
    private int permissionTargetsCount;
    private boolean permissionTargetsCap; // flag will cause the ui to show a tooltip

    public PathEffectivePermissions() {
        this.permissionTargets = new ArrayList<>(permissionTargetCap);
        this.permission = new EffectivePermission();
        permissionTargetsCount = 0;
    }

    public PathEffectivePermissions(String username) {
        this();
        this.principal = username;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public List<String> getPermissionTargets() {
        return permissionTargets;
    }

    public void setPermissionTargets(List<String> permissionTargets) {
        this.permissionTargets = permissionTargets;
    }

    public EffectivePermission getPermission() {
        return permission;
    }

    public void setPermission(EffectivePermission permission) {
        this.permission = permission;
    }

    public boolean isPermissionTargetsCap() {
        return permissionTargets.size() == permissionTargetCap;
    }

    public void setPermissionTargetsCap(boolean permissionTargetsCap) {
        this.permissionTargetsCap = permissionTargetsCap;
    }

    public static int getPermissionTargetCap() {
        return permissionTargetCap;
    }

    public int getPermissionTargetsCount() {
        return permissionTargetsCount;
    }

    public void setPermissionTargetsCount(int permissionTargetsCount) {
        this.permissionTargetsCount = permissionTargetsCount;
    }

    public void advancePermissionTargetsCount() {
        permissionTargetsCount++;
    }
}

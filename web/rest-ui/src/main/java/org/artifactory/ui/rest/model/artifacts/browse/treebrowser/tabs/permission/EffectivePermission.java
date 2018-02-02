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

import org.artifactory.security.AceInfo;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author Chen Keinan
 */
@JsonIgnoreProperties("hasAtLeastOnePermission")
public class EffectivePermission {

    private String principal;
    private boolean delete;
    private boolean deploy;
    private boolean annotate;
    private boolean read;
    private Boolean managed;
    private boolean hasAtLeastOnePermission;
    private Integer mask;

    public EffectivePermission() {
    }

    public EffectivePermission(AceInfo aceInfo) {
        principal = aceInfo.getPrincipal();
        delete = aceInfo.canDelete();
        deploy = aceInfo.canDeploy();
        annotate = aceInfo.canAnnotate();
        read = aceInfo.canRead();
        managed = aceInfo.canManage();
        mask = aceInfo.getMask();
    }

    public void aggregatePermissions(EffectivePermission otherPermission) {
        setRead(isRead() || otherPermission.isRead());
        setAnnotate(isAnnotate() || otherPermission.isAnnotate());
        setDeploy(isDeploy() || otherPermission.isDeploy());
        setDelete(isDelete() || otherPermission.isDelete());
    }

    public void aggregatePermissions(AceInfo aceInfo) {
        setRead(isRead() || aceInfo.canRead());
        setAnnotate(isAnnotate() || aceInfo.canAnnotate());
        setDeploy(isDeploy() || aceInfo.canDeploy());
        setDelete(isDelete() || aceInfo.canDelete());
    }

    public boolean isHasAtLeastOnePermission() {
        return hasAtLeastOnePermission;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        if (delete){
            hasAtLeastOnePermission = true;
        }
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
        if (deploy){
            hasAtLeastOnePermission = true;
        }
    }

    public boolean isAnnotate() {
        return annotate;
    }

    public void setAnnotate(boolean annotate) {
        this.annotate = annotate;
        if (annotate){
            hasAtLeastOnePermission = true;
        }
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
        if (read){
            hasAtLeastOnePermission = true;
        }
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public Boolean isManaged() {
        return managed;
    }

    public void setManaged(Boolean managed) {
        this.managed = managed;
    }

    public Integer getMask() {
        return mask;
    }

    public void setMask(Integer mask) {
        this.mask = mask;
    }
}

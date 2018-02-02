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

package org.artifactory.ui.rest.model.builds;

import org.artifactory.api.build.BuildProps;
import org.artifactory.api.build.diff.BuildsDiffPropertyModel;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.rest.common.model.RestPaging;

/**
 * @author Chen Keinan
 */
public class BuildPropsModel extends BaseModel implements RestPaging {

    private String key;
    private String value;
    private String status;
    private String prevValue;
    private String prevKey;

    public BuildPropsModel(BuildsDiffPropertyModel buildProps) {
        this.key = buildProps.getKey();
        this.value = buildProps.getValue();
        this.status = buildProps.getStatus().toString();
        updateArtifactNames(buildProps);
    }

    public BuildPropsModel(BuildProps buildProps) {
        this.key = buildProps.getKey();
        this.value = buildProps.getValue();
        if (buildProps.getStatus() != null) {
            this.status = buildProps.getStatus().toString();
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(String prevValue) {
        this.prevValue = prevValue;
    }

    /**
     * update current and prev build props name base on diff status
     *
     * @param buildProps - build props as updated from db
     */
    private void updateArtifactNames(BuildsDiffPropertyModel buildProps) {
        if (buildProps != null) {
            String status = buildProps.getStatus().toString();
            switch (status) {
                case "Removed": {
                    prevKey = buildProps.getKey();
                    key = "";
                    value = "";
                    prevValue = buildProps.getValue();
                }
                break;
                case "Add": {
                    prevKey = "";
                    prevValue = "";
                }
                break;
                case "Updated": {
                    prevValue = buildProps.getDiffValue();
                    prevKey = key;
                }
                break;
                case "Unchanged": {
                    prevKey = key;
                    prevValue = value;
                }
                break;
            }
        }
    }

    public String getPrevKey() {
        return prevKey;
    }

    public void setPrevKey(String prevKey) {
        this.prevKey = prevKey;
    }
}

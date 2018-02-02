package org.artifactory.rest.common.model.xray;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author nadav yogev
 */
public class XrayEnabledModel extends BaseModel {

    private boolean xrayEnabled;

    public XrayEnabledModel(boolean enabled) {
        setXrayEnabled(enabled);
    }

    public XrayEnabledModel() {}

    public boolean isXrayEnabled() {
        return xrayEnabled;
    }

    public void setXrayEnabled(boolean enabled) {
        xrayEnabled =  enabled;
    }
}

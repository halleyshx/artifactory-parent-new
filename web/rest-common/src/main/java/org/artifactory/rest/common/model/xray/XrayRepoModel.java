package org.artifactory.rest.common.model.xray;

import org.artifactory.addon.xray.XrayRepo;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class XrayRepoModel extends BaseModel implements XrayRepo {
    private String name;
    private String pkgType;
    private String type;

    public XrayRepoModel(){
        // for jackson
    }

    public XrayRepoModel(String name,String pkgType,String type){
        this.name = name;
        this.pkgType = pkgType;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkgType() {
        return pkgType;
    }

    public void setPkgType(String pkgType) {
        this.pkgType = pkgType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

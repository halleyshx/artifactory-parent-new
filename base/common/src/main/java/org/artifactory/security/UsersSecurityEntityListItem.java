package org.artifactory.security;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsersSecurityEntityListItem extends SecurityEntityListItem {
    private String realm;

    public UsersSecurityEntityListItem() {
    }

    public UsersSecurityEntityListItem(String name, String uri, String realm) {
        super(name, uri);
        this.realm = realm;
    }
}
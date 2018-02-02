package org.artifactory.storage.security.service;

import org.artifactory.security.AclInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author nadavy
 */
public class AclCache {

    // Map of user/group name to a map of repo path to aclInfo
    private Map<String, Map<String, Set<AclInfo>>> groupResultMap;
    private Map<String, Map<String, Set<AclInfo>>> userResultMap;

    public AclCache(Map<String, Map<String, Set<AclInfo>>> groupResultMap,
            Map<String, Map<String, Set<AclInfo>>> userResultMap) {
        this.groupResultMap = groupResultMap;
        this.userResultMap = userResultMap;
    }

    public Map<String, Map<String, Set<AclInfo>>> getGroupResultMap() {
        return groupResultMap;
    }

    public Map<String, Map<String, Set<AclInfo>>> getUserResultMap() {
        return userResultMap;
    }

}

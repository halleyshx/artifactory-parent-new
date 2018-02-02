package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission;

import com.google.common.collect.Sets;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.security.AclInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author nadavy
 */
public class PermissionsModel extends BaseModel {

    private Collection<PathEffectivePermissions> userEffectivePermissions;
    private Collection<PathEffectivePermissions> groupEffectivePermissions;
    private List<PermissionTargetInfo> permissionTargets;

    public PermissionsModel() {

    }

    public PermissionsModel(Collection<PathEffectivePermissions> userEffectivePermissions,
            Collection<PathEffectivePermissions> groupEffectivePermissions, List<AclInfo> aclInfos) {
        this.userEffectivePermissions = userEffectivePermissions;
        this.groupEffectivePermissions = groupEffectivePermissions;
        this.permissionTargets = aclInfos.stream()
                .map(this::getPermissionTargetInfo)
                .collect(Collectors.toList());
    }

    public List<PermissionTargetInfo> getPermissionTargets() {
        return permissionTargets;
    }

    public Collection<PathEffectivePermissions> getUserEffectivePermissions() {
        return userEffectivePermissions;
    }

    public Collection<PathEffectivePermissions> getGroupEffectivePermissions() {
        return groupEffectivePermissions;
    }

    private PermissionTargetInfo getPermissionTargetInfo(AclInfo aclInfo) {
        PermissionTargetInfo permissionTargetInfo = new PermissionTargetInfo();
        permissionTargetInfo.setPermissionName(aclInfo.getPermissionTarget().getName());
        Set<String> groups = Sets.newHashSet();
        Set<String> users = Sets.newHashSet();
        aclInfo.getAces().forEach(aceInfo -> {
            if (aceInfo.isGroup()) {
                groups.add(aceInfo.getPrincipal());
            } else {
                users.add(aceInfo.getPrincipal());
            }
        });
        permissionTargetInfo.setGroups(groups);
        permissionTargetInfo.setUsers(users);
        permissionTargetInfo.setRepoKeys(aclInfo.getPermissionTarget().getRepoKeys());
        return permissionTargetInfo;
    }
}

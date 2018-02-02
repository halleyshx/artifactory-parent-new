package org.artifactory.storage.db.security.service.access;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.*;
import org.jfrog.access.common.ResourceType;
import org.jfrog.access.model.PermissionPrincipalType;
import org.jfrog.access.rest.imports.ImportPermissionRequest;
import org.jfrog.access.rest.permission.*;
import org.jfrog.access.util.ClockUtils;
import org.jfrog.common.JsonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Noam Shemesh
 */
public class AclMapper {

    public static AclInfo toArtifactoryAcl(Permission permission, String serviceId) {
        return InfoFactoryHolder.get().createAcl(
                toPermissionTarget(permission, serviceId),
                toAces(permission.getActions()),
                fromCustomData(permission.getCustomData()).updatedBy);
    }

    private static PermissionTargetInfo toPermissionTarget(Permission permission, String serviceId) {
        CustomData customData = fromCustomData(permission.getCustomData());
        MutablePermissionTargetInfo target = InfoFactoryHolder.get().createPermissionTarget(
                permission.getName().replace(serviceId + ":", ""), customData.repoKeys);
        target.setExcludesPattern(customData.excludePattern);
        target.setIncludesPattern(customData.includePattern);

        return target;
    }

    private static Set<AceInfo> toAces(PermissionActions actions) {
        Set<AceInfo> result = new HashSet<>();
        addEntrySetToAce(result, actions.getGroupActions(), true);
        addEntrySetToAce(result, actions.getUserActions(), false);

        return result;
    }

    private static void addEntrySetToAce(Set<AceInfo> result, Map<String, List<String>> userActions, boolean isGroup) {
        userActions.entrySet()
                .stream()
                .map(entry -> createAce(entry.getKey(), entry.getValue(), isGroup))
                .sequential()
                .collect(Collectors.toCollection(() -> result));
    }

    private static AceInfo createAce(String name, List<String> actions, boolean isGroup) {
        MutableAceInfo ace = InfoFactoryHolder.get().createAce();
        ace.setPrincipal(name);
        ace.setPermissionsFromString(new HashSet<>(actions));
        ace.setGroup(isGroup);

        return ace;
    }

    private static CustomData fromCustomData(String customData) {
        if (customData == null) {
            return new CustomData();
        }
        return JsonUtils.getInstance().readValue(customData, CustomData.class);
    }

    public static PermissionRequest toAccessPermission(AclInfo entity, String serviceId) {
        return toAccessPermission(PermissionRequest.create(), entity, serviceId);
    }

    public static UpdatePermissionRequest toUpdatedAccessPermission(AclInfo entity, String serviceId) {
        return toAccessPermission(UpdatePermissionRequest.create(), entity, serviceId);
    }

    @SuppressWarnings("unchecked")
    private static <T extends PermissionRequest> T toAccessPermission(T object, AclInfo entity, String serviceId) {
        return (T) object
                .name(toAccessName(serviceId, entity.getPermissionTarget().getName()))
                .displayName(entity.getPermissionTarget().getName())
                .serviceId(serviceId)
                .resourceType(ResourceType.REPO)
                .customData(toCustomData(entity))
                .actions(toActions(entity.getAces()));
    }

    public static ImportPermissionRequest toFullAccessPermission(AclInfo entity, String serviceId) {
        ImportPermissionRequest.Builder builder = ImportPermissionRequest.builder()
                .name(toAccessName(serviceId, entity.getPermissionTarget().getName()))
                .displayName(entity.getPermissionTarget().getName())
                .serviceId(serviceId)
                .resourceType(ResourceType.SERVICE)
                .customData(toCustomData(entity))
                .created(ClockUtils.epochMillis())
                .modified(ClockUtils.epochMillis());
        addAccessActions(builder, entity);
        return builder.build();
    }

    public static String toAccessName(String serviceId, String permissionName) {
        return serviceId + ":" + permissionName;
    }

    private static void addAccessActions(ImportPermissionRequest.Builder builder, AclInfo entity) {
        entity.getAces().forEach(ace -> {
            PermissionPrincipalType principalType = ace.isGroup() ? PermissionPrincipalType.GROUP : PermissionPrincipalType.USER;
            ace.getPermissionsAsString().forEach(action -> builder.addAction(action, ace.getPrincipal(), principalType));
        });
    }

    private static PermissionActionsRequest toActions(Set<AceInfo> aces) {
        PermissionActionsRequest actions = PermissionActionsRequest.create();

        aces.forEach(ace -> ace.getPermissionsAsString().forEach(permission -> {
                    if (ace.isGroup()) {
                        actions.addGroupAction(ace.getPrincipal(), permission);
                    } else {
                        actions.addUserAction(ace.getPrincipal(), permission);
                    }
                }));
        return actions;
    }

    private static String toCustomData(AclInfo entity) {
        return JsonUtils.getInstance().valueToString(new CustomData(entity.getUpdatedBy(),
                entity.getPermissionTarget().getExcludesPattern(),
                entity.getPermissionTarget().getIncludesPattern(),
                entity.getPermissionTarget().getRepoKeys()
        ));
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class CustomData {
        String updatedBy;
        String excludePattern;
        String includePattern;
        List<String> repoKeys = new LinkedList<>();

        public CustomData() {}

        CustomData(String updatedBy, String excludePattern, String includePattern,
                List<String> repoKeys) {
            this.updatedBy = updatedBy;
            this.excludePattern = excludePattern;
            this.includePattern = includePattern;
            this.repoKeys = repoKeys;
        }
    }
}

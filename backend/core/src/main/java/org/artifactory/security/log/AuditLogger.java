/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.security.log;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.config.ConfigurationChangesInterceptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.security.interceptor.SecurityConfigurationChangesAdapter;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfrog.client.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.artifactory.security.log.AuditLogEntry.SecurityAction.*;

/**
 * Security Audit Trail logger listens on security and general config changes and logs them to it's own separate log file.
 * The log format is 'performing user|remote address|action performed|json|
 * The json contains what changes were made \ affected principals etc.'
 *
 * @author Dan Feldman
 */
@Component
public class AuditLogger extends SecurityConfigurationChangesAdapter implements ConfigurationChangesInterceptor {
    // private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    //TODO [by dan]: NOP logger will not write anything until we sort out the audit log's format. [RTFACT-9016]
    private static final Logger log =  NOPLogger.NOP_LOGGER;

    private static final String ALL_USERS = "ALL_USERS";
    private ObjectMapper mapper = JacksonFactory.createObjectMapper();

    @Override
    public void onUserAdd(String user) {
        logEntry(new AuditLogEntry(currentUser(), user, null, null, null, CREATE_USER));
    }

    public void externalUserCreated(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, CREATE_EXTERNAL_USER));
    }

    public void userUpdated(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, UPDATE_USER));
    }

    public void userPasswordChanged(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, CHANGE_USER_PASSWORD));
    }

    @Override
    public void onUserDelete(String user) {
        logEntry(new AuditLogEntry(currentUser(), user, null, null, null, DELETE_USER));
    }

    @Override
    public void onAddUsersToGroup(String groupName, List<String> usernames) {
        logEntry(new AuditLogEntry(currentUser(), usernames.toString(), groupName, null, null, ADD_USERS_TO_GROUP));
    }

    @Override
    public void onRemoveUsersFromGroup(String groupName, List<String> usernames) {
        logEntry(new AuditLogEntry(currentUser(), usernames.toString(), groupName, null, null, REMOVE_USERS_FROM_GROUP));
    }

    @Override
    public void onGroupAdd(String groupName) {
        logEntry(new AuditLogEntry(currentUser(), null, groupName, null, null, ADD_GROUP));
    }

    @Override
    public void onGroupDelete(String groupName) {
        logEntry(new AuditLogEntry(currentUser(), null, groupName, null, null, DELETE_GROUP));
    }

    @Override
    public void onPermissionsAdd(AclInfo added) {
        logEntry(new AuditLogEntry(currentUser(), usersFromTarget(added), groupsFromTarget(added),
                targetAffectedRepos(added), added.getPermissionTarget().getName(), ADD_PERMISSION_TARGET));
    }

    @Override
    public void onPermissionsUpdate(final AclInfo updated) {
        logEntry(new AuditLogEntry(currentUser(), usersFromTarget(updated), groupsFromTarget(updated),
                targetAffectedRepos(updated), updated.getPermissionTarget().getName(), UPDATE_PERMISSION_TARGET));
    }

    @Override
    public void onPermissionsDelete(String deleted) {
        logEntry(new AuditLogEntry(currentUser(), null, null, null, deleted, DELETE_PERMISSION_TARGET));
    }

    @Override
    public void onBeforeSave(CentralConfigDescriptor newDescriptor) {
        logEntry(new AuditLogEntry(currentUser(), null, null, null, null, UPDATE_CONFIG_DESCRIPTOR));
    }

    @Override
    public void onAfterSave(CentralConfigDescriptor newDescriptor, CentralConfigDescriptor oldDescriptor) {
        //TODO [by dan]:
    }

    public void configurationEncrypted() {
        logEntry(new AuditLogEntry(currentUser(), null, null, null, null, ENCRYPT_CONFIG_DESCRIPTOR));
    }

    public void configurationDecrypted() {
        logEntry(new AuditLogEntry(currentUser(), null, null, null, null, DECRYPT_CONFIG_DESCRIPTOR));
    }

    public void createApiKey(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, GENERATE_API_KEY));
    }

    public void updateApiKey(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, UPDATE_API_KEY));
    }

    public void revokeApiKey(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, REVOKE_API_KEY));
    }

    public void revokeAllApiKeys() {
        logEntry(new AuditLogEntry(currentUser(), ALL_USERS, null, null, null, REVOKE_ALL_API_KEYS));
    }

    public void expireUserCredentials(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, EXPIRE_CREDENTIALS));
    }

    public void expireAllUserCredentials() {
        logEntry(new AuditLogEntry(currentUser(), ALL_USERS, null, null, null, EXPIRE_ALL_CREDENTIALS));
    }

    public void unexpireUserPassword(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, UNEXPIRE_PASSWORD));
    }

    public void unexpireAllUserPasswords() {
        logEntry(new AuditLogEntry(currentUser(), ALL_USERS, null, null, null, UNEXPIRE_ALL_PASSWORDS));
    }

    public void lockUser(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, LOCK_USER));
    }

    public void unlockUser(String userName) {
        logEntry(new AuditLogEntry(currentUser(), userName, null, null, null, UNLOCK_USER));
    }

    public void unlockAllUsers() {
        logEntry(new AuditLogEntry(currentUser(), ALL_USERS, null, null, null, UNLOCK_ALL_USERS));
    }

    public void unlockAllAdminUsers() {
        logEntry(new AuditLogEntry(currentUser(), "All_ADMIN_USERS", null, null, null, UNLOCK_USER));
    }

    public void loggedInUsernameTrimmed(String trimmedUsername, String originalUsername) {
        logEntry(new AuditLogEntry(trimmedUsername, originalUsername, null, null, null, LOGGED_IN_USERNAME_TRIMMED));
    }

    private void logEntry(AuditLogEntry entry) {
        log.info(entry.getLogEntry(mapper));
    }

    private String currentUser() {
        return ContextHelper.get().beanForType(AuthorizationService.class).currentUsername();
    }

    private String usersFromTarget(final AclInfo acl) {
        return PathUtils.collectionToDelimitedString(acl.getAces().stream()
                .filter(Objects::nonNull)
                .filter(aceInfo -> !aceInfo.isGroup())
                .map(AceInfo::getPrincipal)
                .collect(Collectors.toList()));
    }

    private String groupsFromTarget(final AclInfo acl) {
        return PathUtils.collectionToDelimitedString(acl.getAces().stream()
                .filter(Objects::nonNull)
                .filter(AceInfo::isGroup)
                .map(AceInfo::getPrincipal)
                .collect(Collectors.toList()));
    }

    private String targetAffectedRepos(final AclInfo acl) {
        return PathUtils.collectionToDelimitedString(acl.getPermissionTarget().getRepoKeys());
    }
}

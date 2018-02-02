/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.security.log;


import org.apache.commons.lang.StringUtils;
import org.artifactory.security.AuthenticationHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Dan Feldman
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
class AuditLogEntry {
    private static final Logger log = LoggerFactory.getLogger(AuditLogEntry.class);

    private String performingUser;
    private String affectedUsers;
    private String affectedGroups;
    private String affectedPermissionTarget;
    private String affectedRepos;
    private String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
    private SecurityAction actionPerformed;

    AuditLogEntry(String performingUser, @Nullable String affectedUsers, @Nullable String affectedGroups,
            @Nullable String affectedRepos, @Nullable String affectedPermissionTarget, SecurityAction actionPerformed) {
        this.performingUser = performingUser;
        this.affectedUsers = affectedUsers;
        this.affectedGroups = affectedGroups;
        this.affectedPermissionTarget = affectedPermissionTarget;
        this.actionPerformed = actionPerformed;
        this.affectedRepos = affectedRepos;
    }

    String getLogEntry(ObjectMapper mapper) {
        try {
            return performingUser + "|" + (StringUtils.isBlank(remoteAddress)  ? "" : remoteAddress) + "|"
                    + actionPerformed + "|" + mapper.writeValueAsString(this);
        } catch (IOException e) {
            String err = "Error serializing json entry to audit log: ";
            log.error(err + e.getMessage());
            log.debug(err, e);
        }
        return "";
    }


    public String getAffectedUsers() {
        return affectedUsers;
    }

    public String getAffectedGroups() {
        return affectedGroups;
    }

    public String getAffectedPermissionTarget() {
        return affectedPermissionTarget;
    }

    public String getAffectedRepos() {
        return affectedRepos;
    }

    enum SecurityAction {
        //users
        CREATE_USER, CREATE_EXTERNAL_USER, UPDATE_USER, CHANGE_USER_PASSWORD, DELETE_USER,
        //groups
        ADD_GROUP, DELETE_GROUP, ADD_USERS_TO_GROUP, REMOVE_USERS_FROM_GROUP,
        //permission targets
        ADD_PERMISSION_TARGET, UPDATE_PERMISSION_TARGET, DELETE_PERMISSION_TARGET,
        //api key
        GENERATE_API_KEY, UPDATE_API_KEY, REVOKE_API_KEY, REVOKE_ALL_API_KEYS,
        //lock/expire/unlock/unexpire
        LOCK_USER, UNLOCK_USER, UNLOCK_ALL_USERS, EXPIRE_CREDENTIALS, EXPIRE_ALL_CREDENTIALS,
        UNEXPIRE_PASSWORD, UNEXPIRE_ALL_PASSWORDS,
        //config descriptor
        UPDATE_CONFIG_DESCRIPTOR, ENCRYPT_CONFIG_DESCRIPTOR, DECRYPT_CONFIG_DESCRIPTOR,
        //login
        LOGGED_IN_USERNAME_TRIMMED;
    }
}

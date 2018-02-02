package org.artifactory.api.security;

/**
 * Rating a repo based on how much of it is readable by a user
 *
 * @author Yuval Reches
 */
public enum PermissionHeuristicScore {
    readNotAllowed, readWithExclusion, readAll, admin
}

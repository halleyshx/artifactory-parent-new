/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.storage.db.security.entity;

/**
 * The link between a User and a User Property (users -> user_props)
 *
 * @deprecated Users and groups are in access
 * @author Shay Yaakov
 */
@Deprecated
public class UserProp {
    private final long userId;
    private final String propKey;
    private final String propVal;

    public UserProp(long userId, String propKey, String propVal) {
        if (userId <= 0L) {
            throw new IllegalArgumentException("User id cannot be zero or negative!");
        }
        this.userId = userId;
        this.propKey = propKey;
        this.propVal = propVal;
    }

    public long getUserId() {
        return userId;
    }

    public String getPropKey() {
        return propKey;
    }

    public String getPropVal() {
        return propVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserProp userProp = (UserProp) o;

        if (userId != userProp.userId) return false;
        if (propKey != null ? !propKey.equals(userProp.propKey) : userProp.propKey != null) return false;
        return propVal != null ? propVal.equals(userProp.propVal) : userProp.propVal == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (userId ^ (userId >>> 32));
        result = 31 * result + (propKey != null ? propKey.hashCode() : 0);
        result = 31 * result + (propVal != null ? propVal.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserProp{" +
                "userId=" + userId +
                ", propKey='" + propKey + '\'' +
                ", propVal='" + propVal + '\'' +
                '}';
    }
}

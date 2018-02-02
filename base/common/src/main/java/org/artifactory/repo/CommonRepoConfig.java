/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo;

/**
 * Interface with the most basic methods that are common to the local/remote/virtual repository configurations.
 * This interface is used for returning the user the repository configurations through the REST-API and must not contain
 * any information that is readable by admin user, but not by a regular user.
 *
 * @author Shay Bagants
 */
public interface CommonRepoConfig {

    String getDescription();

    String getType();

    String getPackageType();

    String getKey();
}

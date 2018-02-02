/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Remote repository configuration class that contains information that should be available to non-admin user with
 * partial permission on the repository.
 *
 * @author Shay Bagants
 */
public class PublicRemoteRepoConfig implements CommonRepoConfig {

    private String key;
    private String type;
    private String packageType;
    private String description;
    private String url;

    public PublicRemoteRepoConfig(String key, String type, String packageType, String description, String url) {
        this.key = key;
        this.type = type;
        this.packageType = packageType;
        this.description = description;
        this.url = url;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @JsonProperty(RepositoryConfiguration.TYPE_KEY)
    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getPackageType() {
        return packageType;
    }

    @Override
    public String getKey() {
        return key;
    }

    public String getURL() {
        return url;
    }
}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Virtual repository configuration class that contains information that should be available to non-admin user with
 * partial permission on the repository.
 *
 * @author Shay Bagants
 */
public class PublicVirtualRepoConfig implements CommonRepoConfig {

    private String key;
    private String type;
    private String packageType;
    private String description;
    private List<String> repositories;

    public PublicVirtualRepoConfig(String key, String type, String packageType, String description, List<String> repositories) {
        this.key = key;
        this.type = type;
        this.packageType = packageType;
        this.description = description;
        this.repositories = repositories;
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

    public List<String> getRepositories() {
        return repositories;
    }
}

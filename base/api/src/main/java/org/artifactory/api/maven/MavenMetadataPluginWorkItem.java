/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.maven;

import org.artifactory.api.repo.WorkItem;

import javax.annotation.Nonnull;

/**
 * @author gidis
 */
public class MavenMetadataPluginWorkItem implements WorkItem {
    private final String localRepo;

    public MavenMetadataPluginWorkItem(String localRepo) {
        this.localRepo = localRepo;
    }

    public String getLocalRepo() {
        return localRepo;
    }

    @Override
    public String toString() {
        return "MavenMetadataPluginWorkItem{" +
                "localRepo='" + localRepo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenMetadataPluginWorkItem that = (MavenMetadataPluginWorkItem) o;

        return localRepo != null ? localRepo.equals(that.localRepo) : that.localRepo == null;

    }

    @Override
    public int hashCode() {
        return localRepo != null ? localRepo.hashCode() : 0;
    }

    @Override
    @Nonnull
    public String getUniqueKey() {
        return localRepo;
    }
}

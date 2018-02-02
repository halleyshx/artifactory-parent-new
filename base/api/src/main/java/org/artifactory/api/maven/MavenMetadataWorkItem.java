/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.maven;

import org.artifactory.api.repo.WorkItem;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;

/**
 * @author gidis
 */
public class MavenMetadataWorkItem implements WorkItem {
    private final RepoPath repoPath;
    private final boolean recursive;

    public MavenMetadataWorkItem(RepoPath repoPath, boolean recursive) {
        this.repoPath = repoPath;
        this.recursive = recursive;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public String toString() {
        return "MavenMetadataWorkItem{" +
                "repoPath=" + repoPath +
                ", recursive=" + recursive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenMetadataWorkItem that = (MavenMetadataWorkItem) o;
        if (recursive != that.recursive) return false;
        return repoPath.equals(that.repoPath);

    }

    @Override
    public int hashCode() {
        int result = repoPath.hashCode();
        result = 31 * result + (recursive ? 1 : 0);
        return result;
    }

    @Override
    @Nonnull
    public String getUniqueKey() {
        return this.toString();
    }
}

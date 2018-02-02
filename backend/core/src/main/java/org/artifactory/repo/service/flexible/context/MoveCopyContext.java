/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.context;

import com.google.common.collect.Lists;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;

import java.util.List;

/**
 * @author gidis
 */
public class MoveCopyContext {
    private boolean executeMavenMetadataCalculation;
    private RepoPath sourceRepoPath;
    private String targetKey;
    private String targetPath;
    private Properties addProps;
    private List<String> removeProps;
    private boolean dryRun;
    private boolean failFast;
    private int transactionSize;
    private boolean copy;
    private boolean pruneEmptyFolders;
    private boolean unixStyleBehavior;
    private boolean suppressLayouts;
    private List<MoveCopyItemInfo> foldersToDelete = Lists.newArrayList();

    public RepoPath getSourceRepoPath() {
        return sourceRepoPath;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public Properties getAddProps() {
        return addProps;
    }

    public List<String> getRemoveProps() {
        return removeProps;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public int getTransactionSize() {
        return transactionSize;
    }

    public boolean isCopy() {
        return copy;
    }

    public boolean isExecuteMavenMetadataCalculation() {
        return executeMavenMetadataCalculation;
    }

    public boolean isPruneEmptyFolders() {
        return pruneEmptyFolders;
    }

    public boolean isUnixStyleBehavior() {
        return unixStyleBehavior;
    }

    public boolean isSuppressLayouts() {
        return suppressLayouts;
    }

    public MoveCopyContext(RepoPath sourceRepoPath, String targetKey, String targetPath) {
        this.sourceRepoPath = sourceRepoPath;
        this.targetKey = targetKey;
        this.targetPath = targetPath;
    }

    public MoveCopyContext setCopy(boolean copy) {
        this.copy = copy;
        return this;
    }

    public MoveCopyContext setFailFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public MoveCopyContext setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public MoveCopyContext setExecuteMavenMetadataCalculation(boolean executeMavenMetadataCalculation) {
        this.executeMavenMetadataCalculation = executeMavenMetadataCalculation;
        return this;
    }

    public MoveCopyContext setPruneEmptyFolders(boolean pruneEmptyFolders) {
        this.pruneEmptyFolders = pruneEmptyFolders;
        return this;
    }

    public MoveCopyContext setRemovProperties(List<String> removeProperties) {
        this.removeProps = removeProperties;
        return this;
    }

    public MoveCopyContext setAddProperties(Properties addProperties) {
        this.addProps = addProperties;
        return this;
    }

    public MoveCopyContext setUnixStyleBehavior(boolean unixStyleBehavior) {
        this.unixStyleBehavior = unixStyleBehavior;
        return this;
    }

    public MoveCopyContext setSuppressLayouts(boolean suppressLayouts) {
        this.suppressLayouts = suppressLayouts;
        return this;
    }

    public MoveCopyContext setTransactionSize(int transactionSize) {
        this.transactionSize = transactionSize;
        return this;
    }

    public List<MoveCopyItemInfo> getFoldersToDelete() {
        return foldersToDelete;
    }
}

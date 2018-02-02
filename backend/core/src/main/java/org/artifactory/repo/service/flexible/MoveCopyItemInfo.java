package org.artifactory.repo.service.flexible;

import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.storage.fs.MutableVfsItem;

/**
 * Min
 * @author gidis
 */
public class MoveCopyItemInfo {
    private RepoPath sourceRepoPath;
    private RepoPath targetRepoPath;
    private VfsItem sourceItem;
    private VfsItem targetItem;
    private RepoRepoPath<LocalRepo> sourceRrp;
    private RepoRepoPath<LocalRepo> targetRrp;
    private long dept;
    private MutableVfsItem mutableTargetItem;

    public MoveCopyItemInfo(RepoPath sourceRepoPath,RepoPath targetRepoPath, VfsItem sourceItem, VfsItem targetItem,
                            RepoRepoPath<LocalRepo> sourceRrp, RepoRepoPath<LocalRepo> targetRrp, long dept) {
        this.sourceRepoPath = sourceRepoPath;
        this.targetRepoPath = targetRepoPath;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.sourceRrp = sourceRrp;
        this.targetRrp = targetRrp;
        this.dept = dept;
    }

    public long getDept() {
        return dept;
    }

    public VfsItem getSourceItem() {
        return sourceItem;
    }

    public VfsItem getTargetItem() {
        return targetItem;
    }



    public RepoRepoPath<LocalRepo> getTargetRrp() {
        return targetRrp;
    }

    public RepoPath getSourceRepoPath() {
        return sourceRepoPath;
    }

    public RepoPath getTargetRepoPath() {
        return targetRepoPath;
    }

    public MutableVfsItem getMutableTargetItem() {
        return mutableTargetItem;
    }

    public void setMutableTargetItem(MutableVfsItem mutableTargetItem) {
        this.mutableTargetItem = mutableTargetItem;
    }

    public RepoRepoPath<LocalRepo> getSourceRrp() {
        return sourceRrp;
    }


}

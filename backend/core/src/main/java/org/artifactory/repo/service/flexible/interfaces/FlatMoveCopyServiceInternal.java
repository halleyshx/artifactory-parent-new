/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.interfaces;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;
import org.artifactory.sapi.common.Lock;

import java.util.List;

/**
 * @author gidis
 */
public interface FlatMoveCopyServiceInternal {

    @Lock
    void executeDeleteRootDir(RepoRepoPath<LocalRepo> rrp, MoveCopyContext context, MoveMultiStatusHolder status);

    @Lock
    MoveMultiStatusHolder executeMoveCopyOnBulk(List<MoveCopyItemInfo> items, MoveCopyContext context);


}

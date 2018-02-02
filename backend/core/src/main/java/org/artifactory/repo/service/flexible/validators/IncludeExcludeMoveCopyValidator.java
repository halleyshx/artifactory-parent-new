/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.validators;

import org.apache.http.HttpStatus;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gidis
 */
public class IncludeExcludeMoveCopyValidator implements MoveCopyValidator {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationMoveValidator.class);

    @Override
    public boolean validate(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context) {
        RepoRepoPath<LocalRepo> targetRrp = element.getTargetRrp();
        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        String targetPath = targetRepoPath.getPath();

        if (!targetRepo.accepts(targetRepoPath)) {
            status.error("The repository '" + targetRepo.getKey() + "' rejected the resolution of artifact in path '" + targetPath
                    + "' due to a conflict with its include/exclude patterns.", HttpStatus.SC_FORBIDDEN, log);
            return false;
        }
        return true;
    }

    @Override
    public boolean isInterested(MoveCopyItemInfo element, MoveCopyContext context) {
        return true;
    }
}

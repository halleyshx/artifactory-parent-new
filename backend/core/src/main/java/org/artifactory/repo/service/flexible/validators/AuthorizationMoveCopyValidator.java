/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.validators;

import org.apache.http.HttpStatus;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gidis
 */
public class AuthorizationMoveCopyValidator implements MoveCopyValidator {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationMoveCopyValidator.class);
    private AuthorizationService authService;

    public AuthorizationMoveCopyValidator(AuthorizationService authService) {
        this.authService = authService;
    }

    @Override
    public boolean validate(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context) {
        VfsItem targetItem = element.getTargetItem();
        RepoRepoPath<LocalRepo> targetRrp = element.getTargetRrp();
        boolean valid = validateInternal(element, status, context);
        if (!valid && (targetItem == null || targetItem.isFile())) {
            // target repo doesn't accept this path and it doesn't already contain it OR the target is a file
            // so there is no point to continue to the children
            status.error("Cannot create/override the path '" + targetRrp.getRepoPath() + "'. " +
                    "Skipping this path and all its children.", log);
        }
        return valid;
    }

    private boolean validateInternal(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context) {
        RepoPath targetRepoPath = element.getTargetRepoPath();
        if (element.getTargetItem() != null) {
            if (!authService.canDelete(targetRepoPath)) {
                status.error("User doesn't have permissions to override '" + targetRepoPath + "'. " +
                        "Needs delete permissions.", HttpStatus.SC_UNAUTHORIZED, log);
                return false;
            }
        } else if (!authService.canDeploy(targetRepoPath)) {
            status.error("User doesn't have permissions to create '" + targetRepoPath + "'. " +
                    "Needs write permissions.", HttpStatus.SC_FORBIDDEN, log);
            return false;
        }
        // all tests passed
        return true;
    }

    @Override
    public boolean isInterested(MoveCopyItemInfo itemInfo, MoveCopyContext context) {
        return true;
    }
}

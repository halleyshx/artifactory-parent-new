/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.validators;

import org.apache.http.HttpStatus;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gidis
 */
public class AuthorizationMoveValidator implements MoveCopyValidator {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationMoveValidator.class);
    private AuthorizationService authService;

    public AuthorizationMoveValidator(AuthorizationService authService) {
        this.authService = authService;
    }

    @Override
    public boolean validate(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context) {
        RepoPath sourceRepoPath = element.getSourceItem().getRepoPath();
        // Check permission to delete source repoPath
        if (!context.isCopy() && !authService.canDelete(sourceRepoPath)) {
            status.error("User doesn't have permissions to move '" + sourceRepoPath + "'. " +
                    "Needs delete permissions.", HttpStatus.SC_FORBIDDEN, log);
            return false;
        }
        // All tests passed
        return true;
    }

    @Override
    public boolean isInterested(MoveCopyItemInfo element, MoveCopyContext context) {
        return ! context.isCopy();
    }
}

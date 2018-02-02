/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.validators;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;

/**
 * @author gidis
 */
public interface MoveCopyValidator {
    boolean validate(MoveCopyItemInfo itemInfo, MoveMultiStatusHolder status, MoveCopyContext context);
    boolean isInterested(MoveCopyItemInfo itemInfo, MoveCopyContext context);
}

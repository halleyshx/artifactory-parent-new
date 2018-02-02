/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.interfaces;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;

/**
 * @author gidis
 */
public interface FlatMoveCopyService {

    MoveMultiStatusHolder moveCopy(MoveCopyContext context);

}

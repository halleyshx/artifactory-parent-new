/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.flexible.validators;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LayoutsCoreAddon;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.flexible.MoveCopyItemInfo;
import org.artifactory.repo.service.flexible.context.MoveCopyContext;

/**
 * @author gidis
 */
public class MoveLayoutValidator implements MoveCopyValidator {
    private final LayoutsCoreAddon layoutsCoreAddon;

    public MoveLayoutValidator(AddonsManager addonsManager) {
        layoutsCoreAddon = addonsManager.addonByType(LayoutsCoreAddon.class);
    }

    @Override
    public boolean validate(MoveCopyItemInfo element, MoveMultiStatusHolder status, MoveCopyContext context) {
        // Check if cross layout move/copy
        LocalRepo sourceRepo = element.getSourceRrp().getRepo();
        LocalRepo targetRepo = element.getTargetRrp().getRepo();
        RepoLayout sourceLayout = sourceRepo.getDescriptor().getRepoLayout();
        RepoLayout targetLayout = targetRepo.getDescriptor().getRepoLayout();
        if (!layoutsCoreAddon.canCrossLayouts(sourceLayout, targetLayout)) {
            throw new IllegalArgumentException(String.format("Can't execute cross layout move, layouts (source %s and target %s)" +
                    "are not equals", sourceLayout, targetLayout));
        }
        return true;
    }

    @Override
    public boolean isInterested(MoveCopyItemInfo itemInfo, MoveCopyContext context) {
        return ! context.isSuppressLayouts();
    }
}

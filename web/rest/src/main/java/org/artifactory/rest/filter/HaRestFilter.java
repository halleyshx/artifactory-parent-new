/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.rest.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.exception.HaNodePropagationException;

/**
 * @author Shay Yaakov
 */
public class HaRestFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        HaCommonAddon haAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaCommonAddon.class);
        if (haAddon.isHaEnabled()) {
            String nodeId = containerRequest.getHeaderValue(HaCommonAddon.ARTIFACTORY_NODE_ID);
            if (StringUtils.isNotBlank(nodeId) && StringUtils.contains(containerRequest.getRequestUri().getPath(), "/mc/")) {
                if (!StringUtils.equals(haAddon.getCurrentMemberServerId(), nodeId)) {
                    throw new HaNodePropagationException(containerRequest, nodeId);
                }
            }
        }
        return containerRequest;
    }
}

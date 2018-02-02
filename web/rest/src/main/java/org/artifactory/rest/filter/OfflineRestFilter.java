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
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.api.context.ContextHelper;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

/**
 * author: gidis
 * Block Rest request during offline state
 */

public class OfflineRestFilter implements ContainerRequestFilter {

    @Context
    HttpServletResponse response;

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        // Filter out all events in case of offline mode
        if (ContextHelper.get().isOffline()) {
            throw new AuthorizationRestException();
        }
        return containerRequest;
    }
}

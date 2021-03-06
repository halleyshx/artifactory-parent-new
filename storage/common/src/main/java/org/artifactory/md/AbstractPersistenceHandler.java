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

package org.artifactory.md;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.storage.spring.StorageContextHelper;

/**
 * @author freds
 */
public abstract class AbstractPersistenceHandler<T, M> implements MetadataPersistenceHandler<T, M> {

    private AuthorizationService authService;
    private final XmlMetadataProvider<T, M> xmlProvider;

    protected AbstractPersistenceHandler(XmlMetadataProvider<T, M> xmlProvider) {
        this.xmlProvider = xmlProvider;
    }

    protected String getMetadataName() {
        return xmlProvider.getMetadataName();
    }

    protected AuthorizationService getAuthorizationService() {
        if (authService == null) {
            authService = StorageContextHelper.get().getAuthorizationService();
        }
        return authService;
    }
}

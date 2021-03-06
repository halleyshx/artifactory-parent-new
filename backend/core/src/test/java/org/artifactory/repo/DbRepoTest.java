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

package org.artifactory.repo;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.db.DbLocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.util.RepoLayoutUtils;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for the DbRepo.
 *
 * @author Noam Tenne
 */
public class DbRepoTest extends ArtifactoryHomeBoundTest {

    @AfterClass
    public void tearDown() {
        ArtifactoryContextThreadBinder.unbind();
    }

    @Test
    public void testNoFilePermission() {
        AuthorizationService authService = EasyMock.createMock(AuthorizationService.class);
        XrayAddon xrayAddon = createNiceMock(XrayAddon.class);
        AddonsManager addonsManager = createNiceMock(AddonsManager.class);

        InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(context.getAuthorizationService()).andReturn(authService);
        expect(context.beanForType(AddonsManager.class)).andReturn(addonsManager);
        expect(addonsManager.addonByType(XrayAddon.class)).andReturn(xrayAddon);
        expect(xrayAddon.isDownloadBlocked(anyObject(RepoPath.class))).andReturn(false);
        EasyMock.replay(context, addonsManager);
        ArtifactoryContextThreadBinder.bind(context);

        InternalRepositoryService irs = EasyMock.createMock(InternalRepositoryService.class);
        EasyMock.replay(irs);

        LocalRepoDescriptor lrd = new LocalRepoDescriptor();
        lrd.setKey("libs");
        lrd.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
        DbLocalRepo dbLocalRepo = new DbLocalRepo<>(lrd, irs, null);
        RepoPath path = InternalRepoPathFactory.create("libs", "jfrog/settings/jfrog-settings-sources.zip");
        EasyMock.expect(authService.canRead(path)).andReturn(false);
        EasyMock.expect(authService.currentUsername()).andReturn("testUser");
        EasyMock.replay(authService);

        StatusHolder holder = dbLocalRepo.checkDownloadIsAllowed(path);
        Assert.assertTrue(holder.isError(), "User should not have access to src files");
        EasyMock.verify(context, authService, irs);
    }
}

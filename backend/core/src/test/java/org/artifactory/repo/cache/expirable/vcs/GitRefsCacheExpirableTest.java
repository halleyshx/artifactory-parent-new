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

package org.artifactory.repo.cache.expirable.vcs;

import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.repo.LocalCacheRepo;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class GitRefsCacheExpirableTest {

    public void testExpirable() throws Exception {
        LocalCacheRepoDescriptor descriptor = createMock(LocalCacheRepoDescriptor.class);

        LocalCacheRepo repo = createMock(LocalCacheRepo.class);
        expect(repo.getDescriptor()).andReturn(descriptor).anyTimes();

        replay(descriptor, repo);

        GitRefsCacheExpirable expirable = new GitRefsCacheExpirable();
        assertFalse(expirable.isExpirable(repo, "refs.tar.gz"));
        assertFalse(expirable.isExpirable(repo, "gitrefs.tgz"));
        assertFalse(expirable.isExpirable(repo, "gitrefs.zip"));
        assertTrue(expirable.isExpirable(repo, "gitrefs"));
        assertTrue(expirable.isExpirable(repo, "bla/gitrefs"));
        assertTrue(expirable.isExpirable(repo, "twbs/bootstrap/gitrefs"));

        verify(descriptor, repo);
    }
}
package org.artifactory.storage.db.security.service;

import com.google.common.collect.Sets;
import org.artifactory.model.xstream.security.AceImpl;
import org.artifactory.model.xstream.security.AclImpl;
import org.artifactory.model.xstream.security.PermissionTargetImpl;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.security.ArtifactoryPermission;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.storage.security.service.AclStoreService;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * UnitTest of AclCacheLoader, especially the call method
 * mock the dao to create AclCache
 *
 * @author nadavy
 */

@Test
public class AclCacheLoaderTest {

    private static final String USERNAME1 = "user1";
    private static final String USERNAME2 = "user2";
    private static final String USERNAME3 = "user3";
    private static final String GROUPNAME1 = "group1";
    private static final String GROUPNAME2 = "group2";

    private static final String REPO1 = "repo1";
    private static final String REPO2 = "repo2";

    private AclCacheLoader.AclCacheItem aclCacheItem;

    /**
     * Create DAO mocks, populate new AclCache and call AclCache.
     */
    @BeforeClass
    public void populateAclInfo() {
        Collection<AclInfo> aclInfos = Lists.newArrayList();
        aclInfos.add(getAnyAcl());
        aclInfos.add(getRepo1Acl());

        AclStoreService aclStoreService = EasyMock.createMock(AclStoreService.class);

        EasyMock.expect(aclStoreService.getDownstreamAllAcls()).andReturn(aclInfos).anyTimes();

        EasyMock.replay(aclStoreService);

        AclCacheLoader cacheLoader = new AclCacheLoader(aclStoreService);
        aclCacheItem = cacheLoader.call();
    }

    /**
     * create readers ACL, with user1 and user2.
     */
    private AclInfo getAnyAcl() {
        AceImpl user1 = new AceImpl(USERNAME1, false, 0);
        AceImpl user2 = new AceImpl(USERNAME2, false, 0);
        Set<AceInfo> aces = Sets.newHashSet(user1, user2);

        PermissionTargetImpl pt = new PermissionTargetImpl("readerT",
                Lists.newArrayList(PermissionTargetInfo.ANY_LOCAL_REPO),
                Lists.newArrayList("**"),
                Lists.newArrayList(""));

        return new AclImpl(pt, aces, "me");
    }

    /**
     * create deployers ACL, with user2, user3 and group1
     */
    private AclImpl getRepo1Acl() {
        AceImpl user2 = new AceImpl(USERNAME2, false, ArtifactoryPermission.DEPLOY.getMask());
        AceImpl user3 = new AceImpl(USERNAME3, false, ArtifactoryPermission.DEPLOY.getMask());
        AceImpl group = new AceImpl(GROUPNAME1, true, ArtifactoryPermission.DEPLOY.getMask());
        Set<AceInfo> aces = Sets.newHashSet(user2, user3, group);

        PermissionTargetImpl pt = new PermissionTargetImpl("deployT", Lists.newArrayList(REPO1, REPO2),
                Lists.newArrayList("**"), Lists.newArrayList("a/**"));

        return new AclImpl(pt, aces, "me");
    }

    /**
     * Assert the different AclCacheLoader caches- groups and users
     */
    public void testAclCacheLoader() {
        Map<String, Map<String, Set<AclInfo>>> groupResultMap = aclCacheItem.getGroupResultMap();
        assertGroupMap(groupResultMap);

        Map<String, Map<String, Set<AclInfo>>> userResultMap = aclCacheItem.getUserResultMap();
        assertUserMap(userResultMap);

    }

    /**
     * assert that user1 and user2 - read permission on any local repo
     * user2 and user3 2 - deploy permissions on repo1 (except a/**)
     */
    private void assertUserMap(Map<String, Map<String, Set<AclInfo>>> userResultMap) {
        assertTrue(userResultMap.size() == 3, "UserAclMap should have 3 users");

        Map<String, Set<AclInfo>> user1RepoToAclMap = userResultMap.get(USERNAME1);
        Map<String, Set<AclInfo>> user2RepoToAclMap = userResultMap.get(USERNAME2);
        Map<String, Set<AclInfo>> user3RepoToAclMap = userResultMap.get(USERNAME3);

        assertTrue(user1RepoToAclMap.size() == 1, "User1 should have permission on ANY");
        assertTrue(user2RepoToAclMap.size() == 3, "User2 should have ANY, REPO1 and REPO2");
        assertTrue(user3RepoToAclMap.size() == 2, "User3 should have REPO1 and REPO2");

        assertTrue(user1RepoToAclMap.get(PermissionTargetInfo.ANY_LOCAL_REPO) != null,
                "User1 should have permission on ANY");
        assertTrue(user2RepoToAclMap.get(PermissionTargetInfo.ANY_LOCAL_REPO) != null,
                "User1 should have permission on ANY");
        assertTrue(user2RepoToAclMap.get(REPO1) != null, "User2 should have permission on REPO1");
        assertTrue(user2RepoToAclMap.get(REPO2) != null, "User2 should have permission on REPO2");
        assertTrue(user3RepoToAclMap.get(REPO1) != null, "User3 should have permission on REPO1");
        assertTrue(user3RepoToAclMap.get(REPO2) != null, "User3 should have permission on REPO2");

        // check include/exclude
        assertTrue("a/**".equals(
                user3RepoToAclMap.get(REPO1).iterator().next().getPermissionTarget().getExcludesPattern()));
        assertTrue("**".equals(
                user3RepoToAclMap.get(REPO1).iterator().next().getPermissionTarget().getIncludesPattern()));
    }

    /**
     * assert that group1 is in 1 acl, has deploy permission on repo1 only
     * group2 should have any acls
     */
    private void assertGroupMap(Map<String, Map<String, Set<AclInfo>>> groupRepoToAclMap) {
        assertTrue(groupRepoToAclMap.size() == 1);
        Map<String, Set<AclInfo>> groupMapToAcl = groupRepoToAclMap.get(GROUPNAME1);
        Set<AclInfo> groupRepoAcls = groupMapToAcl.get(REPO1); // group1 has 1 acl, which consists only of REPO1
        assertTrue(groupRepoAcls != null, "GROUP1 shouldn't be null");
        assertTrue(groupRepoAcls.size() == 1, "GROUP1 should have only 1 ACL");
        Set<AceInfo> groupAces = groupRepoAcls.iterator().next().getAces();
        assertEquals(groupAces.size(), 3, "GROUP1 ACL should contain 3 ACEs"); // 2nd with user3
        assertNull(groupRepoToAclMap.get(GROUPNAME2), "GROUP2 don't have ACLs ");
    }
}

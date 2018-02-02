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

package org.artifactory.security;

import junit.framework.AssertionFailedError;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.security.ResetPasswordException;
import org.artifactory.api.security.SecurityListener;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.*;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.environment.converter.local.PreInitConverter;
import org.artifactory.exception.InvalidNameException;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.model.xstream.security.UserImpl;
import org.artifactory.repo.*;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.access.emigrate.AccessSecurityEmigrator;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.security.interceptor.ApiKeysEncryptor;
import org.artifactory.security.interceptor.BintrayAuthEncryptor;
import org.artifactory.security.interceptor.UserPasswordEncryptor;
import org.artifactory.security.log.AuditLogger;
import org.artifactory.storage.security.service.AclCache;
import org.artifactory.storage.security.service.AclStoreService;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.storage.security.service.UserLockInMemoryService;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.util.NameValidator;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.easymock.EasyMock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * SecurityServiceImpl unit tests. TODO: simplify the tests
 *
 * @author Yossi Shaul
 */
@Test
public class SecurityServiceImplTest extends ArtifactoryHomeBoundTest {

    private SecurityContextImpl securityContext;
    private SecurityServiceImpl service;
    private List<AclInfo> testAcls;
    private AclStoreService aclStoreServiceMock;
    private InternalRepositoryService repositoryServiceMock;
    private LocalRepo localRepoMock;
    private LocalRepo cacheRepoMock;
    private InternalCentralConfigService centralConfigServiceMock;
    private UserGroupStoreService userGroupStoreService;
    private SecurityListener securityListenerMock;
    private AuditLogger auditLogMock;
    private SecurityServiceImplTestHelper securityServiceImplTestHelper;
    private AclCache aclCache;
    private ApiKeysEncryptor apiKeysEncryptorMock;
    private UserPasswordEncryptor userPasswordEncryptorMock;
    private BintrayAuthEncryptor bintrayAuthEncryptorMock;
    private UserPassAuthenticationProvider userPassAuthenticationProvider;
    private AccessSecurityEmigrator securityEmigrator;
    private UserLockInMemoryService userLockInMemoryService;

    @BeforeClass
    public void initArtifactoryRoles() {
        securityServiceImplTestHelper = new SecurityServiceImplTestHelper();
        testAcls = securityServiceImplTestHelper.createTestAcls();
        aclCache = securityServiceImplTestHelper.createUserAndGroupResultMap();
        aclStoreServiceMock = createMock(AclStoreService.class);
        repositoryServiceMock = createRepoServiceMock();
        centralConfigServiceMock = createMock(InternalCentralConfigService.class);
        userGroupStoreService = createMock(UserGroupStoreService.class);
        localRepoMock = createLocalRepoMock();
        cacheRepoMock = createCacheRepoMock();
        securityListenerMock = createMock(SecurityListener.class);
        auditLogMock = createMock(AuditLogger.class);
        apiKeysEncryptorMock = createMock(ApiKeysEncryptor.class);
        userPasswordEncryptorMock = createMock(UserPasswordEncryptor.class);
        bintrayAuthEncryptorMock = createMock(BintrayAuthEncryptor.class);
        userPassAuthenticationProvider = createMock(UserPassAuthenticationProvider.class);
        securityEmigrator = createMock(AccessSecurityEmigrator.class);
        userLockInMemoryService = createMock(UserLockInMemoryService.class);
    }

    @BeforeMethod
    public void setUp() {
        // create new security context
        securityContext = new SecurityContextImpl();
        SecurityContextHolder.setContext(securityContext);

        // new service instance
        service = new SecurityServiceImpl();
        // set the aclManager mock on the security service
        ReflectionTestUtils.setField(service, "userGroupStoreService", userGroupStoreService);
        ReflectionTestUtils.setField(service, "aclStoreService", aclStoreServiceMock);
        ReflectionTestUtils.setField(service, "repositoryService", repositoryServiceMock);
        ReflectionTestUtils.setField(service, "centralConfig", centralConfigServiceMock);
        ReflectionTestUtils.setField(service, "auditLog", auditLogMock);
        ReflectionTestUtils.setField(service, "apiKeysEncryptor", apiKeysEncryptorMock);
        ReflectionTestUtils.setField(service, "userPasswordEncryptor", userPasswordEncryptorMock);
        ReflectionTestUtils.setField(service, "bintrayAuthEncryptor", bintrayAuthEncryptorMock);
        ReflectionTestUtils.setField(service, "userPassAuthenticationProvider", userPassAuthenticationProvider);
        ReflectionTestUtils.setField(service, "securityEmigrator", securityEmigrator);
        ReflectionTestUtils.setField(service, "userLockInMemoryService", userLockInMemoryService);

        // reset mocks
        reset(aclStoreServiceMock, repositoryServiceMock, centralConfigServiceMock, auditLogMock, apiKeysEncryptorMock, userPasswordEncryptorMock, bintrayAuthEncryptorMock, securityEmigrator, userLockInMemoryService);
    }

    public void isAdminOnAdminUser() {
        Authentication authentication = setAdminAuthentication();

        boolean admin = service.isAdmin();
        assertTrue(admin, "The user in test is admin");
        // un-authenticate
        authentication.setAuthenticated(false);
        admin = service.isAdmin();
        assertFalse(admin, "Unauthenticated token");
    }

    public void isAdminOnSimpleUser() {
        setSimpleUserAuthentication();

        boolean admin = service.isAdmin();
        assertFalse(admin, "The user in test is not an admin");
    }

    @Test(dependsOnMethods = "isAdminOnAdminUser")
    public void spidermanCanDoAnything() {
        setAdminAuthentication();
        assertFalse(service.isAnonymous());// sanity
        assertTrue(service.isAdmin());// sanity

        RepoPath path = InternalRepoPathFactory.create("someRepo", "blabla");
        boolean canRead = service.canRead(path);
        assertTrue(canRead);
        boolean canDeploy = service.canDeploy(path);
        assertTrue(canDeploy);
    }

    @Test
    public void userReadAndDeployPermissions() {
        setSimpleUserAuthentication();

        RepoPath securedPath = InternalRepoPathFactory.create("securedRepo", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(securedPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey(securedPath.getRepoKey())).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);

        // cannot read the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean hasPermissions =
                service.canRead(securedPath) || service.canManage(securedPath) || service.canDeploy(securedPath) ||
                        service.canDelete(securedPath);
        assertFalse(hasPermissions, "User should not have permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);

        boolean canDeploy = service.canDeploy(securedPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(aclStoreServiceMock, repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        RepoPath allowedReadPath = InternalRepoPathFactory.create("testRepo1", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(allowedReadPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey(allowedReadPath.getRepoKey())).andReturn(null)
                .anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);

        // can read the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        hasPermissions = service.canRead(allowedReadPath);
        assertTrue(hasPermissions, "User should have read permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(aclStoreServiceMock, repositoryServiceMock);
        reset(aclStoreServiceMock);

        // cannot admin the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean canAdmin = service.canManage(allowedReadPath);
        assertFalse(canAdmin, "User should not have permissions for this path");
        verify(aclStoreServiceMock);
    }

    @Test
    public void adminRolePermissions() {
        // user with admin role on permission target 'target1'
        setSimpleUserAuthentication("yossis");

        RepoPath allowedReadPath = InternalRepoPathFactory.create("testRepo1", "blabla");

        // can read the specified path
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expectGetAllAclsCall();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);

        // can deploy to the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);

        // can admin the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean canAdmin = service.canManage(allowedReadPath);
        assertTrue(canAdmin, "User should have permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);
    }

    @Test
    public void groupPermissions() {
        RepoPath allowedReadPath = InternalRepoPathFactory.create("testRepo1", "**");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(allowedReadPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should have permissions for this path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);

        // add the user to a group with permissions and expext permission garnted
        setSimpleUserAuthentication("userwithnopermissions", "deployGroup");
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User in a group with permissions for this path");
        reset(aclStoreServiceMock, repositoryServiceMock);
    }

    @Test
    public void userWithPermissionsToAGroupWithTheSameName() {
        setSimpleUserAuthentication(securityServiceImplTestHelper.USER_AND_GROUP_SHARED_NAME,
                securityServiceImplTestHelper.USER_AND_GROUP_SHARED_NAME);

        RepoPath testRepo1Path = InternalRepoPathFactory.create("testRepo1", "**");
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        RepoPath testRepo2Path = InternalRepoPathFactory.create("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo2")).andReturn(null).anyTimes();
        replay(aclStoreServiceMock, repositoryServiceMock);
        canRead = service.canRead(testRepo2Path);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);
    }

    @Test
    public void userWithPermissionsToANonUniqueGroupName() {
        // here we test that a user that belongs to a group which has
        // the same name of a nother user will only get the group permissions
        // and not the user with the same name permissions
        setSimpleUserAuthentication("auser", securityServiceImplTestHelper.USER_AND_GROUP_SHARED_NAME);

        RepoPath testRepo1Path = InternalRepoPathFactory.create("testRepo1", "**");
        expectGetAllAclsCallWithAnyArray();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1"))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        RepoPath testRepo2Path = InternalRepoPathFactory.create("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2"))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo2")).andReturn(null).anyTimes();
        replay(aclStoreServiceMock, repositoryServiceMock);
        canRead = service.canRead(testRepo2Path);
        verify(repositoryServiceMock);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
    }

    @Test
    public void hasPermissionPassingUserInfo() {
        SimpleUser user = createNonAdminUser("yossis");
        UserInfo userInfo = user.getDescriptor();

        RepoPath testRepo1Path = InternalRepoPathFactory.create("testRepo1", "any/path");

        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canRead = service.canRead(userInfo, testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canDeploy = service.canDeploy(userInfo, testRepo1Path);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canDelete = service.canDelete(userInfo, testRepo1Path);
        assertFalse(canDelete, "User should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canAdmin = service.canManage(userInfo, testRepo1Path);
        assertTrue(canAdmin, "User should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        RepoPath testRepo2Path = InternalRepoPathFactory.create("testRepo2", "**");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo2")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        canRead = service.canRead(userInfo, testRepo2Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setAnonAccessEnabled(false);

        CentralConfigDescriptor configDescriptor = createMock(CentralConfigDescriptor.class);
        expect(configDescriptor.getSecurity()).andReturn(securityDescriptor).anyTimes();
        replay(configDescriptor);
        expect(centralConfigServiceMock.getDescriptor()).andReturn(configDescriptor).anyTimes();
        replay(centralConfigServiceMock);

        SimpleUser anon = createNonAdminUser(UserInfo.ANONYMOUS);
        UserInfo anonUserInfo = anon.getDescriptor();

        RepoPath testMultiRepo = InternalRepoPathFactory.create("multi1", "**");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi1")).andReturn(cacheRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(repositoryServiceMock);

        canRead = service.canRead(anonUserInfo, testMultiRepo);
        verify(repositoryServiceMock);
        assertFalse(canRead, "Anonymous user should have permissions for this path");
    }

    @Test
    public void hasPermissionWithSpecificTarget() {
        SimpleUser user = createNonAdminUser("shay");
        UserInfo userInfo = user.getDescriptor();
        RepoPath testRepo1Path = InternalRepoPathFactory.create("specific-repo", "com", true);

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("specific-repo")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("specific-repo")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        boolean canRead = service.canRead(userInfo, testRepo1Path);
        assertTrue(canRead, "User should have read permissions for this path");
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canDeploy = service.canDeploy(userInfo, testRepo1Path);
        assertTrue(canDeploy, "User should have deploy permissions for this path");
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canDelete = service.canDelete(userInfo, testRepo1Path);
        assertFalse(canDelete, "User should not have delete permissions for this path");
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canAdmin = service.canManage(userInfo, testRepo1Path);
        assertFalse(canAdmin, "User should not have admin permissions for this path");
        reset(aclStoreServiceMock, repositoryServiceMock);
    }

    @Test
    public void hasPermissionForGroupInfo() {
        GroupInfo groupInfo = InfoFactoryHolder.get().createGroup("deployGroup");

        RepoPath testRepo1Path = InternalRepoPathFactory.create("testRepo1", "any/path");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1"))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canRead = service.canRead(groupInfo, testRepo1Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canDeploy = service.canDeploy(groupInfo, testRepo1Path);
        assertTrue(canDeploy, "Group should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canDelete = service.canDelete(groupInfo, testRepo1Path);
        assertFalse(canDelete, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        boolean canAdmin = service.canManage(groupInfo, testRepo1Path);
        assertFalse(canAdmin, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        RepoPath testRepo2Path = InternalRepoPathFactory.create("testRepo2", "some/path");

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2"))
                .andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRepo2")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        canRead = service.canRead(groupInfo, testRepo2Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        GroupInfo anyRepoGroupRead = InfoFactoryHolder.get().createGroup("anyRepoReadersGroup");
        RepoPath somePath = InternalRepoPathFactory.create("blabla", "some/path");

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("blabla")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("blabla")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        canRead = service.canRead(anyRepoGroupRead, somePath);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        canDeploy = service.canDeploy(anyRepoGroupRead, somePath);
        assertFalse(canDeploy, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);

        GroupInfo multiRepoGroupRead = InfoFactoryHolder.get().createGroup("multiRepoReadersGroup");
        RepoPath multiPath = InternalRepoPathFactory.create("multi1", "some/path");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi1")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("multi1")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi2")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("multi2")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, repositoryServiceMock);
        canRead = service.canRead(multiRepoGroupRead, multiPath);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        RepoPath multiPath2 = InternalRepoPathFactory.create("multi2", "some/path");
        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        canRead = service.canRead(multiRepoGroupRead, multiPath2);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(repositoryServiceMock);
        reset(aclStoreServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock);
        canDeploy = service.canDeploy(multiRepoGroupRead, multiPath);
        assertFalse(canDeploy, "Group should not have permissions for this path");
        verify(repositoryServiceMock);
    }

    @Test
    public void getAllPermissionTargetsForAdminUser() {
        setAdminAuthentication();
        Map<String, Set<AclInfo>> adminRepoToAclMap = aclCache.getUserResultMap().get("yossis");
        for (String repoPath : adminRepoToAclMap.keySet()) {
            RepoPath path = RepoPathFactory.create(repoPath);
            assertTrue(service.canManage(path), "Admin should be manager of all repos in his cache map");
        }
        assertTrue(service.canManage(RepoPathFactory.create("multi1")),
                "Admin should be manager of repos not belonging to his cache map");
    }

    @Test
    public void getAllPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("noadminpermissionsuser");

        expectAclScan();

        List<PermissionTargetInfo> permissionTargets = service.getPermissionTargets(ArtifactoryPermission.MANAGE);
        assertEquals(permissionTargets.size(), 0);

        verify(aclStoreServiceMock);
    }

    @Test(enabled = false)
    public void getDeployPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("user");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        assertEquals(targets.size(), 0);

        verify(aclStoreServiceMock);
    }

    @Test
    public void getDeployPermissionTargetsForUserWithDeployPermission() {
        // yossis should have testRepo1 and testRemote-cache
        setSimpleUserAuthentication("yossis");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        assertEquals(targets.size(), 2, "Expecting two deploy permission");

        verify(aclStoreServiceMock);
    }

    @Test
    public void userPasswordMatches() {
        setSimpleUserAuthentication("user");

        assertTrue(service.userPasswordMatches("password"));
        assertFalse(service.userPasswordMatches(""));
        assertFalse(service.userPasswordMatches("Password"));
        assertFalse(service.userPasswordMatches("blabla"));
    }

    @Test
    public void permissionOnRemoteRoot() {
        setSimpleUserAuthentication();

        expect(repositoryServiceMock.repositoryByKey("testRemote")).andReturn(createRemoteRepoMock()).anyTimes();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRemote-cache")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("testRemote-cache")).andReturn(null).anyTimes();
        replay(repositoryServiceMock);

        // cannot read the specified path
        expectGetAllAclsCall();
        replay(aclStoreServiceMock);
        boolean hasPermissionOnRemoteRoot = service.userHasPermissionsOnRepositoryRoot("testRemote");
        assertTrue(hasPermissionOnRemoteRoot, "User should have permissions for this path");
        verify(aclStoreServiceMock, repositoryServiceMock);
        reset(aclStoreServiceMock, repositoryServiceMock);
    }

    @Test(dependsOnMethods = {"permissionOnRemoteRoot"},
            expectedExceptions = {InvalidNameException.class})
    protected void findOrCreateExternalAuthUserWithIllegalChars() {
        reset(userGroupStoreService);
        String userName = new String(NameValidator.getForbiddenChars());
        expect(userGroupStoreService.findUser(userName)).andReturn(null).anyTimes();
        replay(userGroupStoreService);
        service.findOrCreateExternalAuthUser(userName, false);
    }

    @DataProvider
    public static Object[][] provideAccessEmigrateData() {
        return new Object[][]{
                { new CompoundVersionDetails(ArtifactoryVersion.v540, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true},
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true},
                { new CompoundVersionDetails(ArtifactoryVersion.v560m005, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), false},
        };
    }

    @DataProvider
    public static Object[][] provideAccessEmigrateDataMarker() {
        return new Object[][]{
                { new CompoundVersionDetails(ArtifactoryVersion.v540, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), false, false},
                { new CompoundVersionDetails(ArtifactoryVersion.v540, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v560m005, "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "", System.currentTimeMillis()
                        ), true, true},
        };
    }

    @DataProvider
    private static Object[][] provideVersionsApiKeyEncryptor() {
        return new Object[][]{
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v4112, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "456", System.currentTimeMillis()), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v480, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v480, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, false},
        };
    }


    @DataProvider
    private static Object[][] provideVersionsUserPasswordEncryptor() {
        return new Object[][]{
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), true ,true},
                { new CompoundVersionDetails(ArtifactoryVersion.v500, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v4112, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.v4112, "", "", "456", System.currentTimeMillis()), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v4112, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, false},
        };
    }

    @DataProvider
    private static Object[][] provideVersionsBintrayAuthEncryptor() {
        return new Object[][]{
                { new CompoundVersionDetails(ArtifactoryVersion.v300, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), true, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v400, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.v480, "", "", "456", System.currentTimeMillis()), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v125, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, true},
                { new CompoundVersionDetails(ArtifactoryVersion.v480, "", "", "456", System.currentTimeMillis()),
                        new CompoundVersionDetails(ArtifactoryVersion.getCurrent(), "", "", "456", System.currentTimeMillis()
                        ), false, false},
        };
    }

    @Test(dataProvider = "provideVersionsApiKeyEncryptor")
    public void testConvertApiKeyEncryptor(CompoundVersionDetails source, CompoundVersionDetails target, boolean expected,  boolean encrypt) throws IOException {
        File key = new File(getBound().getEtcDir().getAbsolutePath() + "/security/artifactory.key");
        apiKeysEncryptorMock.encryptOrDecryptAsynchronously(encrypt);

        if (encrypt && expected) {
            EasyMock.expectLastCall().once();
            key.createNewFile();
        } else {
            if (!encrypt) {
                key.delete();
            }
            if (!expected) {
                EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
            }
        }

        userPasswordEncryptorMock.encryptOrDecryptAsynchronously(encrypt);
        EasyMock.expectLastCall().anyTimes();

        bintrayAuthEncryptorMock.encryptOrDecryptAsynchronously(encrypt);
        EasyMock.expectLastCall().anyTimes();

        replay(apiKeysEncryptorMock, userPasswordEncryptorMock, bintrayAuthEncryptorMock);

        service.convert(source, target);

        EasyMock.verify(apiKeysEncryptorMock);
    }


    @Test(dataProvider = "provideVersionsUserPasswordEncryptor")
    public void testConvertUserPasswordEncryptor(CompoundVersionDetails source, CompoundVersionDetails target, boolean expected, boolean encrypt) throws IOException {
        File key = new File(getBound().getEtcDir().getAbsolutePath() + "/security/artifactory.key");
        userPasswordEncryptorMock.encryptOrDecryptAsynchronously(true);
        if (encrypt && expected) {
            EasyMock.expectLastCall().once();
            key.createNewFile();
        } else {
            if (!encrypt) {
                key.delete();
            }
            if (!expected) {
                EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
            }
        }

        apiKeysEncryptorMock.encryptOrDecryptAsynchronously(true);
        EasyMock.expectLastCall().anyTimes();

        bintrayAuthEncryptorMock.encryptOrDecryptAsynchronously(true);
        EasyMock.expectLastCall().anyTimes();

        replay(apiKeysEncryptorMock, userPasswordEncryptorMock, bintrayAuthEncryptorMock);

        service.convert(source, target);

        EasyMock.verify(userPasswordEncryptorMock);
    }


    @Test(dataProvider = "provideVersionsBintrayAuthEncryptor")
    public void testConvertBintrayAuthEncryptor(CompoundVersionDetails source, CompoundVersionDetails target, boolean expected, boolean encrypt) throws IOException {
        File key = new File(getBound().getEtcDir().getAbsolutePath() + "/security/artifactory.key");
        bintrayAuthEncryptorMock.encryptOrDecryptAsynchronously(true);
        if (encrypt && expected) {
            EasyMock.expectLastCall().once();
            key.createNewFile();
        } else {
            if (!encrypt) {
                key.delete();
            }
            if (!expected) {
                EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
            }
        }

        apiKeysEncryptorMock.encryptOrDecryptAsynchronously(true);
        EasyMock.expectLastCall().anyTimes();

        userPasswordEncryptorMock.encryptOrDecryptAsynchronously(true);
        EasyMock.expectLastCall().anyTimes();

        replay(apiKeysEncryptorMock, userPasswordEncryptorMock, bintrayAuthEncryptorMock);

        service.convert(source, target);

        EasyMock.verify(bintrayAuthEncryptorMock);
    }

    @Test(dataProvider = "provideAccessEmigrateData")
    public void testConvertEmigrateToAccess(CompoundVersionDetails source, CompoundVersionDetails target, boolean expected) throws IOException {
        securityEmigrator.emigrate();
        if (expected) {
            EasyMock.expectLastCall().once();
        } else {
            EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
        }

        replay(securityEmigrator);
        new PreInitConverter(homeStub).convert(source, target);

        assertEquals(homeStub.getAccessEmigrateMarkerFile().exists(), expected);

        service.convert(source, target);

        assertFalse(homeStub.getAccessEmigrateMarkerFile().exists());

        EasyMock.verify(securityEmigrator);
    }

    @Test(dataProvider = "provideAccessEmigrateDataMarker")
    public void testIsIntrested(CompoundVersionDetails source, CompoundVersionDetails target,
            boolean createMarker, boolean expected) throws IOException {
        FileUtils.deleteQuietly(homeStub.getAccessEmigrateMarkerFile());
        if (createMarker) {
            FileUtils.write(homeStub.getAccessEmigrateMarkerFile(), "");
        }

        assertEquals(service.isInterested(source, target), expected);
    }

    public void testUserHasPermissions() {
        // user shouldn't have any permissions
        SimpleUser user = createNonAdminUser("noperm");
        reset(userGroupStoreService);
        expect(userGroupStoreService.findUser("noperm")).andReturn(user.getDescriptor()).once();

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, userGroupStoreService);
        Map<String, Set<AclInfo>> nopermPermMap = aclCache.getUserResultMap().get("noperm");
        boolean hasNoPermissions = nopermPermMap == null || nopermPermMap.size() == 0;
        assertTrue(hasNoPermissions, "User should not have permissions");
        reset(aclStoreServiceMock, userGroupStoreService);
    }

    public void testUserHasPermissionsFromGroup() {
        // user don't have permissions on his own, but should have permission from his group
        SimpleUser user = createNonAdminUser("noperm", securityServiceImplTestHelper.USER_AND_GROUP_SHARED_NAME);
        reset(userGroupStoreService);
        expect(userGroupStoreService.findUser("noperm")).andReturn(user.getDescriptor()).once();

        expectGetAllAclsCallWithAnyArray();
        replay(aclStoreServiceMock, userGroupStoreService);
        Map<String, Set<AclInfo>> nopermPermMap = aclCache.getUserResultMap().get("noperm");
        boolean hasNoPermissions = nopermPermMap == null || nopermPermMap.size() == 0;
        assertTrue(hasNoPermissions, "User shouldn't have permissions on this path");
        hasNoPermissions = user.getDescriptor()
                .getGroups()
                .stream()
                .allMatch(group -> {
                    Map<String, Set<AclInfo>> nopermGroupPermMap = aclCache.getGroupResultMap()
                            .get(group.getGroupName());
                    return nopermGroupPermMap == null || nopermGroupPermMap.size() == 0;
                });
        assertFalse(hasNoPermissions, "User should have permissions on this path from his groups");
        reset(aclStoreServiceMock, userGroupStoreService);
    }

    public void userReadPermissionsOnAnyRemote() {
        // user should have a read (only) permission on any remote
        setSimpleUserAuthentication("anyRemoteUser");
        //secureRepo is a cached remote repo
        RepoPath securedPath = InternalRepoPathFactory.create("securedRepo", "blabla");

        expectGetAllAclsCall();
        RemoteRepo remoteRepoMock = createRemoteRepoMock();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("securedRepo")).andReturn(cacheRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepositoryByKey("securedRepo")).andReturn(remoteRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey(securedPath.getRepoKey())).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY LOCAL")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(aclStoreServiceMock, repositoryServiceMock);
        assertTrue(service.canRead(securedPath), "User should have read permissions on any remote path");
        assertFalse(
                service.canDeploy(securedPath) || service.canManage(securedPath) || service.canDelete(securedPath) ||
                        service.canManage(securedPath), "User shouldn't have other permissions on any remote path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);
    }

    public void userDeployPermissionsOnAnyLocal() {
        // user should have deploy permission on any local
        setSimpleUserAuthentication("anyLocalUser");

        RepoPath securedPath = InternalRepoPathFactory.create("securedRepo", "blabla");

        expectGetAllAclsCall();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("securedRepo")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.remoteRepositoryByKey("securedRepo")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey(securedPath.getRepoKey())).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY REMOTE")).andReturn(null).anyTimes();
        expect(repositoryServiceMock.remoteRepoDescriptorByKey("ANY")).andReturn(null).anyTimes();
        replay(aclStoreServiceMock, repositoryServiceMock);

        assertTrue(service.canDeploy(securedPath), "User should have deploy permissions on any local path");
        assertFalse(
                service.canRead(securedPath) || service.canManage(securedPath) || service.canDelete(securedPath) ||
                        service.canManage(securedPath), "User shouldn't have other permissions on any remote path");
        verify(aclStoreServiceMock);
        reset(aclStoreServiceMock);
    }

    public void updateLastLoginWithNotExistingUserTest() throws InterruptedException {
        // Make sure that if the user doesn't exists we stop the "updateLastLogin" process without  exception
        reset(userGroupStoreService);
        // Enable the update last login process "userLastAccessUpdatesResolutionSecs" must be greater or equals to "1"
        getBound().setProperty(ConstantValues.userLastAccessUpdatesResolutionSecs, "1");
        expect(userGroupStoreService.findUser("user")).andReturn(null).once();
        replay(userGroupStoreService);
        service.updateUserLastLogin("user", "momo", System.currentTimeMillis() + 1000);
        reset(userGroupStoreService);
    }

    public void testUserLastLoginTimeUpdateBuffer() throws InterruptedException {
        getBound().setProperty(ConstantValues.userLastAccessUpdatesResolutionSecs, "0");
        service.updateUserLastLogin("user", "momo", System.currentTimeMillis());
        getBound().setProperty(ConstantValues.userLastAccessUpdatesResolutionSecs,
                ConstantValues.userLastAccessUpdatesResolutionSecs.getDefValue());

        MutableUserInfo user = new UserInfoBuilder("user").build();
        user.setLastLoginTimeMillis(0);

        //Simulate No existing last login, expect an update
        expect(userGroupStoreService.findUser("user")).andReturn(user).times(1);
        userGroupStoreService.updateUser(user, true);
        EasyMock.expectLastCall();
        replay(userGroupStoreService);
        service.updateUserLastLogin("user", "momo", System.currentTimeMillis());

        //Give a last login from the near past, expect no update
        long nearPastLogin = System.currentTimeMillis();

        verify(userGroupStoreService);
        reset(userGroupStoreService);
        user = new UserInfoBuilder("user").build();
        user.setLastLoginTimeMillis(nearPastLogin);
        expect(userGroupStoreService.findUser("user")).andReturn(user).once();
        replay(userGroupStoreService);
        service.updateUserLastLogin("user", "momo", nearPastLogin + 100L);

        //Give a last login from the future, expect an update
        verify(userGroupStoreService);
        reset(userGroupStoreService);
        expect(userGroupStoreService.findUser("user")).andReturn(user).times(2);
        userGroupStoreService.updateUser(user, true);
        EasyMock.expectLastCall();
        replay(userGroupStoreService);
        service.updateUserLastLogin("user", "momo", System.currentTimeMillis() + 6000L);
    }

    public void testSelectiveReload() {
        TreeSet<SecurityListener> securityListeners = new TreeSet<>();
        securityListeners.add(securityListenerMock);
        ReflectionTestUtils.setField(service, "securityListeners", securityListeners);
        reset(securityListenerMock);
        securityListenerMock.onClearSecurity();
        expect(securityListenerMock.compareTo(securityListenerMock)).andReturn(0).anyTimes();
        replay(securityListenerMock);

        SecurityDescriptor newSecurityDescriptor = new SecurityDescriptor();
        SecurityDescriptor oldSecurityDescriptor = new SecurityDescriptor();
        oldSecurityDescriptor.addLdap(new LdapSetting());

        CentralConfigDescriptor newConfigDescriptor = createMock(CentralConfigDescriptor.class);
        expect(newConfigDescriptor.getSecurity()).andReturn(newSecurityDescriptor).anyTimes();
        replay(newConfigDescriptor);

        CentralConfigDescriptor oldConfigDescriptor = createMock(CentralConfigDescriptor.class);
        expect(oldConfigDescriptor.getSecurity()).andReturn(oldSecurityDescriptor).anyTimes();
        replay(oldConfigDescriptor);

        expect(centralConfigServiceMock.getDescriptor()).andReturn(newConfigDescriptor).anyTimes();
        replay(centralConfigServiceMock);

        service.reload(oldConfigDescriptor);
        verify(securityListenerMock);

        // The security conf is the same, so onClearSecurity should NOT be called
        service.reload(newConfigDescriptor);
        verify(securityListenerMock);
        ReflectionTestUtils.setField(service, "securityListeners", null);
    }

    private void expectAclScan() {
        expect(aclStoreServiceMock.getAllAcls()).andReturn(testAcls).anyTimes();
        expect(aclStoreServiceMock.getAclCache()).andReturn(aclCache).anyTimes();
        replay(aclStoreServiceMock);
    }

    private void expectGetAllAclsCall() {
        expect(aclStoreServiceMock.getAllAcls()).andReturn(testAcls).anyTimes();
        expect(aclStoreServiceMock.getAclCache()).andReturn(aclCache).anyTimes();
    }

    private void expectGetAllAclsCallWithAnyArray() {
        expect(aclStoreServiceMock.getAllAcls()).andReturn(testAcls);
        expect(aclStoreServiceMock.getAclCache()).andReturn(aclCache);
    }

    private Authentication setAdminAuthentication() {
        SimpleUser adminUser = createAdminUser();
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                adminUser, null, SimpleUser.ADMIN_GAS);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private Authentication setSimpleUserAuthentication() {
        return setSimpleUserAuthentication("user");
    }

    private Authentication setSimpleUserAuthentication(String username, String... groups) {
        SimpleUser simpleUser = createNonAdminUser(username, groups);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                simpleUser, "password", SimpleUser.USER_GAS);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private static SimpleUser createNonAdminUser(String username, String... groups) {
        UserInfo userInfo = new UserInfoBuilder(username).updatableProfile(true)
                .internalGroups(new HashSet<>(Arrays.asList(groups))).build();
        return new SimpleUser(userInfo);
    }

    private static SimpleUser createAdminUser() {
        UserInfo userInfo = new UserInfoBuilder("spiderman").admin(true).updatableProfile(true).build();
        return new SimpleUser(userInfo);
    }

    private static LocalRepo createLocalRepoMock() {
        LocalRepo localRepo = createMock(LocalRepo.class);
        expect(localRepo.isLocal()).andReturn(true).anyTimes();
        expect(localRepo.isCache()).andReturn(false).anyTimes();
        replay(localRepo);
        return localRepo;
    }

    private static LocalRepo createCacheRepoMock() {
        LocalRepo localRepo = createMock(LocalRepo.class);
        expect(localRepo.isLocal()).andReturn(true).anyTimes();
        expect(localRepo.isCache()).andReturn(true).anyTimes();
        replay(localRepo);
        return localRepo;
    }

    private RemoteRepo createRemoteRepoMock() {
        RemoteRepo remoteRepo = createMock(RemoteRepo.class);
        expect(remoteRepo.isReal()).andReturn(true).anyTimes();
        replay(remoteRepo);
        return remoteRepo;
    }

    private InternalRepositoryService createRepoServiceMock() {
        InternalRepositoryService repositoryService = createMock(InternalRepositoryService.class);
        replay(repositoryService);
        return repositoryService;
    }

    @Test(
            expectedExceptions = {PasswordChangeException.class},
            expectedExceptionsMessageRegExp = "Old password is incorrect"
    )
    public void changePasswordUsingIncorrectOldPassword() {
        SaltedPassword sp = new SaltedPassword("foo", "salt");

        UserInfo user = new UserImpl() {{
            setUsername("test");
            setUpdatableProfile(true);
            setPassword(sp);
        }};

        expect(userGroupStoreService.findUser("test")).andReturn(user).anyTimes();
        expect(userPassAuthenticationProvider.canUserLogin(anyObject(), anyObject())).andReturn(false);

        MutableCentralConfigDescriptor mutableCentralConfigDescriptor = createMock(
                MutableCentralConfigDescriptor.class);
        PasswordExpirationPolicy expirationPolicy = new PasswordExpirationPolicy();
        expirationPolicy.setEnabled(Boolean.TRUE);

        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setPasswordSettings(new PasswordSettings() {{
            setExpirationPolicy(expirationPolicy);
        }});

        expect(centralConfigServiceMock.getDescriptor()).andReturn(mutableCentralConfigDescriptor).anyTimes();
        expect(centralConfigServiceMock.getMutableDescriptor()).andReturn(mutableCentralConfigDescriptor).anyTimes();
        expect(mutableCentralConfigDescriptor.getSecurity()).andReturn(securityDescriptor).anyTimes();
        expect(userLockInMemoryService.getNextLoginDelay("test")).andReturn(-1L).anyTimes();
        userLockInMemoryService.registerIncorrectLoginAttempt("test");
        EasyMock.expectLastCall();
        replay(userGroupStoreService, mutableCentralConfigDescriptor, centralConfigServiceMock);

        service.changePassword("test", "bar", "pass", "pass");
    }

    @Test(
            dependsOnMethods = {"changePasswordUsingIncorrectOldPassword"},
            expectedExceptions = {PasswordChangeException.class},
            expectedExceptionsMessageRegExp = "User test is Locked.\n" +
                    "Contact System Administrator to Unlock The Account."
    )

    public void changePasswordForLockedOutUserTest() {
        SaltedPassword sp = new SaltedPassword("foo", "salt");

        UserInfo user = new UserImpl() {{
            setUsername("test");
            setUpdatableProfile(true);
            setPassword(sp);
        }};
        reset(userGroupStoreService);
        expect(userGroupStoreService.findUser("test")).andReturn(user).anyTimes();

        MutableCentralConfigDescriptor mutableCentralConfigDescriptor = createMock(
                MutableCentralConfigDescriptor.class);
        PasswordExpirationPolicy expirationPolicy = new PasswordExpirationPolicy();
        expirationPolicy.setEnabled(Boolean.TRUE);

        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setPasswordSettings(new PasswordSettings() {{
            setExpirationPolicy(expirationPolicy);
        }});

        UserLockPolicy userLockPolicy = new UserLockPolicy() {{
            setEnabled(Boolean.TRUE);
            setLoginAttempts(1);
        }};
        securityDescriptor.setUserLockPolicy(userLockPolicy);

        expect(centralConfigServiceMock.getDescriptor()).andReturn(mutableCentralConfigDescriptor).anyTimes();
        expect(centralConfigServiceMock.getMutableDescriptor()).andReturn(mutableCentralConfigDescriptor).anyTimes();
        expect(mutableCentralConfigDescriptor.getSecurity()).andReturn(securityDescriptor).anyTimes();
        expect(userGroupStoreService.isUserLocked("test")).andReturn(true);
        replay(userGroupStoreService, mutableCentralConfigDescriptor, centralConfigServiceMock);

        service.changePassword("test", "bar", "pass", "pass");
    }

    @Test
    public void testValidateResetPasswordAttempt() {
        PasswordResetPolicy policy = new PasswordResetPolicy() {{
            setEnabled(true);
            setMaxAttemptsPerAddress(3);
            setTimeToBlockInMinutes(60);
        }};
        PasswordSettings passwordSettings = new PasswordSettings() {{
            setResetPolicy(policy);
        }};
        SecurityDescriptor securityDescriptor = new SecurityDescriptor() {{
            setPasswordSettings(passwordSettings);
        }};

        PasswordResetPolicy oldPolicy = new PasswordResetPolicy();
        PasswordSettings oldPasswordSettings = new PasswordSettings() {{
            setResetPolicy(oldPolicy);
        }};
        SecurityDescriptor oldSecurityDescriptor = new SecurityDescriptor() {{
            setPasswordSettings(oldPasswordSettings);
        }};

        CentralConfigDescriptor oldConfigDescriptorMock = createMock(CentralConfigDescriptor.class);
        expect(oldConfigDescriptorMock.getSecurity()).andReturn(oldSecurityDescriptor).anyTimes();

        CentralConfigDescriptor configDescriptorMock = createMock(CentralConfigDescriptor.class);
        expect(centralConfigServiceMock.getDescriptor()).andReturn(configDescriptorMock).anyTimes();
        expect(configDescriptorMock.getSecurity()).andReturn(securityDescriptor).anyTimes();
        replay(centralConfigServiceMock, configDescriptorMock, oldConfigDescriptorMock);

        service.reload(oldConfigDescriptorMock);

        int timeBetweenAttempts = 600;
        String remoteAddress1 = "remote.address.1." + System.currentTimeMillis();

        service.validateResetPasswordAttempt(remoteAddress1);
        try {
            service.validateResetPasswordAttempt(remoteAddress1);
            fail("Validation should fail due to too frequent requests");
        } catch (ResetPasswordException e) {
            assertTrue(e.getMessage().toLowerCase().contains("too frequent"));
        }
        sleep(timeBetweenAttempts);
        service.validateResetPasswordAttempt(remoteAddress1);
        sleep(timeBetweenAttempts);
        service.validateResetPasswordAttempt(remoteAddress1);
        try {
            service.validateResetPasswordAttempt(remoteAddress1);
            fail("Validation should fail due to too many requests");
        } catch (ResetPasswordException e) {
            assertTrue(e.getMessage().toLowerCase().contains("too many"));
        }

        String remoteAddress2 = "remote.address.2." + System.currentTimeMillis();
        service.validateResetPasswordAttempt(remoteAddress2);
    }

    @Test
    public void testAuthenticateAsSystem() throws Exception {
        service.authenticateAsSystem();
        assertAuthenticatedAsSystem();
    }

    @Test
    public void testDoAsSystem() throws Exception {
        Authentication mockAuthentication = createMock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        service.doAsSystem(this::assertAuthenticatedAsSystem);
        assertEquals(SecurityContextHolder.getContext().getAuthentication(), mockAuthentication, "original authentication was not restored");

        try {
            service.doAsSystem(() -> {
                throw new RuntimeException("expected exception");
            });
            fail("Exception was expected");
        } catch (Exception e) {
            //ignore expected exception
        }
        assertEquals(SecurityContextHolder.getContext().getAuthentication(), mockAuthentication, "original authentication was not restored");
    }

    private void assertAuthenticatedAsSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> InternalSecurityService.ROLE_ADMIN.equals(authority.getAuthority())),
                "Authentication does not apply the admin role");
        assertEquals(((UserDetails)authentication.getPrincipal()).getUsername(), SecurityService.USER_SYSTEM);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package org.artifactory.security.access.emigrate;

import com.google.common.collect.Sets;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.model.xstream.security.*;
import org.artifactory.security.*;
import org.artifactory.storage.db.security.service.access.emigrate.SecurityEmigratorFetchers;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Shemesh
 */
public class AccessSecurityEmigratorTest extends ArtifactoryHomeBoundTest {

    private InternalSecurityService internalSecurityService;
    private SecurityEmigratorFetchers securityEmigratorFetchers;
    private AccessSecurityEmigratorImpl accessSecurityEmigrator;
    private Capture<SecurityInfo> securityInfo;

    @BeforeMethod
    public void beforeMethod() {
        securityEmigratorFetchers = createMock(SecurityEmigratorFetchers.class);
        internalSecurityService = createMock(InternalSecurityService.class);

        accessSecurityEmigrator = new AccessSecurityEmigratorImpl(securityEmigratorFetchers);
        accessSecurityEmigrator.setSecurityService(internalSecurityService);

        securityInfo = new Capture<>();
        internalSecurityService.importSecurityData(capture(securityInfo));
        expectLastCall().once();
    }

    private void regularExpects() {
        expect(securityEmigratorFetchers.getAllUserInfos()).andReturn(getUsers()).once();
        expect(securityEmigratorFetchers.getAllGroupInfos()).andReturn(getGroups()).once();
        expect(securityEmigratorFetchers.getAllAclInfos()).andReturn(getAcls()).once();
    }

    private void emptyExpects() {
        expect(securityEmigratorFetchers.getAllUserInfos()).andReturn(Collections.emptyList()).once();
        expect(securityEmigratorFetchers.getAllGroupInfos()).andReturn(Collections.emptyList()).once();
        expect(securityEmigratorFetchers.getAllAclInfos()).andReturn(Collections.emptyList()).once();
    }

    private List<GroupInfo> getGroups() {
        return Lists.newArrayList(
                new GroupImpl("group")
        );
    }

    private List<UserInfo> getUsers() {
        UserImpl me = new UserImpl("me");
        me.addGroup("group");
        return Lists.newArrayList(
                me,
                new UserImpl("notme")
        );
    }

    private List<AclInfo> getAcls() {
        return Lists.newArrayList(
                new AclImpl(
                        new PermissionTargetImpl("permission-target-1", Lists.newArrayList("repo"), "", "**"),
                        Sets.newHashSet(new AceImpl("group", true, 1), new AceImpl("user", false, 0)),
                        "me"
                ),
                new AclImpl(
                        new PermissionTargetImpl("permission-target-2", Lists.newArrayList("repo2"), "**", ""),
                        Sets.newHashSet(new AceImpl("user3", false, 0)),
                        "notme"
                ));
    }

    @AfterMethod
    public void afterMethod() {
        EasyMock.verify(internalSecurityService, securityEmigratorFetchers);
    }

    @Test
    public void shouldGenerateCorrectSecurityInfo() {
        regularExpects();
        EasyMock.replay(internalSecurityService, securityEmigratorFetchers);

        Arrays.stream(ArtifactoryHome.get().getEtcDir()
                .listFiles(file -> file.getName().startsWith("export.security.")))
                .forEach(File::delete);

        accessSecurityEmigrator.emigrate();

        assertTrue(ArtifactoryHome.get().getEtcDir()
                .listFiles(file -> file.getName().startsWith("export.security.")).length > 0);

        assertEquals(securityInfo.getValue().getUsers().size(), 2);
        assertEquals(securityInfo.getValue().getUsers().stream().map(UserInfo::getUsername).collect(Collectors.toSet()), Sets.newHashSet("me", "notme"));
        assertEquals(securityInfo.getValue().getAcls().size(), 2);
        assertEquals(securityInfo.getValue().getAcls().stream()
                .map(AclInfo::getPermissionTarget)
                .map(PermissionTargetInfo::getName)
                .collect(Collectors.toSet()), Sets.newHashSet("permission-target-1", "permission-target-2"));
        assertEquals(securityInfo.getValue().getGroups().size(), 1);
        assertEquals(securityInfo.getValue().getVersion(), "v9");
    }

    @Test
    public void shouldGenerateEmptySecurityInfo() {
        emptyExpects();
        EasyMock.replay(internalSecurityService, securityEmigratorFetchers);
        accessSecurityEmigrator.emigrate();

        assertEquals(securityInfo.getValue().getUsers().size(), 0);
        assertEquals(securityInfo.getValue().getAcls().size(), 0);
        assertEquals(securityInfo.getValue().getGroups().size(), 0);
        assertEquals(securityInfo.getValue().getVersion(), "v9");
    }
}
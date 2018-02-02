package org.artifactory.storage.db.security.service.access;

import org.artifactory.model.xstream.security.GroupImpl;
import org.artifactory.security.GroupInfo;
import org.artifactory.security.MutableGroupInfo;
import org.jfrog.access.model.Realm;
import org.jfrog.access.rest.group.Group;
import org.jfrog.access.rest.group.GroupRequest;
import org.jfrog.access.rest.group.GroupResponse;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.testng.Assert.*;

/**
 * Units tests of the {@link GroupMapper}.
 *
 * @author Yossi Shaul
 */
@Test
public class GroupMapperTest {

    public void groupInfoToGroup() {
        GroupInfo groupInfo = sampleGroupInfo();
        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertNotNull(accessGroup);
        assertEquals(accessGroup.getName(), groupInfo.getGroupName());
        assertEquals(accessGroup.getDescription(), groupInfo.getDescription());
        assertEquals(accessGroup.isAutoJoin(), groupInfo.isNewUserDefault());
        assertNull(accessGroup.getCustomData());
        assertEquals(accessGroup.getRealm().getName(), groupInfo.getRealm());
        assertEquals(accessGroup.getRealmAttributes(), groupInfo.getRealmAttributes());
    }

    public void groupInfoWithEmptyRealmToGroup() {
        GroupImpl groupInfo = new GroupImpl("northmen");
        groupInfo.setRealm("");

        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertNotNull(accessGroup);
        assertEquals(accessGroup.getRealm(), Realm.INTERNAL);
    }

    public void groupInfoWithNullRealmToGroup() {
        GroupImpl groupInfo = new GroupImpl("northmen");
        groupInfo.setRealm(null);

        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertNotNull(accessGroup);
        assertEquals(accessGroup.getRealm(), Realm.INTERNAL);
    }

    public void groupInfoWithGroupAdminToGroup() {
        MutableGroupInfo groupInfo = sampleGroupInfo();
        groupInfo.setAdminPrivileges(true);
        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertThat(accessGroup.getCustomData()).isNotNull().hasSize(1)
                .includes(entry(GroupMapper.ArtifactoryBuiltInGroupProperty.artifactory_admin.name(), "true"));
    }

    public void groupInfoUnknownRealmToGroup() {
        MutableGroupInfo groupInfo = sampleGroupInfo();
        groupInfo.setRealm("unknown_realm");
        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertEquals(accessGroup.getRealm(), Realm.valueOf(groupInfo.getRealm()));
    }

    public void groupInfoArtifactoryRealmToAccessInternalRealm() {
        MutableGroupInfo groupInfo = sampleGroupInfo();
        groupInfo.setRealm("artifactory");
        GroupRequest accessGroup = GroupMapper.toAccessGroup(groupInfo);
        assertEquals(accessGroup.getRealm(), Realm.INTERNAL, "Old artifactory realm is converted to internal realm");
    }

    public void accessGroupToGroupInfo() {
        Group accessGroup = sampleAccessGroup();
        GroupInfo groupInfo = GroupMapper.toArtifactoryGroup(accessGroup);
        assertNotNull(groupInfo);
        assertEquals(groupInfo.getGroupName(), accessGroup.getName());
        assertEquals(groupInfo.getDescription(), accessGroup.getDescription());
        assertEquals(groupInfo.isNewUserDefault(), accessGroup.isAutoJoin());
        assertFalse(groupInfo.isAdminPrivileges());
        assertEquals(groupInfo.getRealm(), accessGroup.getRealm().getName());
        assertEquals(groupInfo.getRealmAttributes(), accessGroup.getRealmAttributes());
    }

    public void accessInternalGroupToGroupInfo() {
        Group accessGroup = new GroupResponse().name("northmen").description("the northmen").autoJoin(false)
                .realm(Realm.INTERNAL).realmAttributes("a b c");
        GroupInfo groupInfo = GroupMapper.toArtifactoryGroup(accessGroup);
        assertEquals(groupInfo.getRealm(), "internal");
    }

    private MutableGroupInfo sampleGroupInfo() {
        GroupImpl group = new GroupImpl("northmen");
        group.setDescription("the northmen");
        group.setNewUserDefault(false);
        group.setAdminPrivileges(false);
        group.setRealm("something_new");
        group.setRealmAttributes("a b c");
        return group;
    }

    private Group sampleAccessGroup() {
        return new GroupResponse().name("northmen").description("the northmen").autoJoin(false)
                .realm(Realm.LDAP).realmAttributes("a b c");
    }

}
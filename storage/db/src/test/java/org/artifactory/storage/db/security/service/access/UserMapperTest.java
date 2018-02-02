package org.artifactory.storage.db.security.service.access;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.EnumUtils;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.SaltedPassword;
import org.artifactory.security.UserInfo;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.fest.assertions.MapAssert;
import org.jfrog.access.model.Realm;
import org.jfrog.access.model.UserStatus;
import org.jfrog.access.rest.group.GroupResponse;
import org.jfrog.access.rest.user.*;
import org.joda.time.format.ISODateTimeFormat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.artifactory.storage.db.security.service.access.UserMapper.ArtifactoryBuiltInUserProperty.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link UserMapper}.
 *
 * @author Yossi Shaul
 */
@Test
public class UserMapperTest extends ArtifactoryHomeBoundTest {

    @BeforeMethod
    public void setup() {
        if (CryptoHelper.hasArtifactoryKey(ArtifactoryHome.get())) {
            CryptoHelper.removeArtifactoryKeyFile(ArtifactoryHome.get());
        }
    }

    public void fullUserToUserInfo() throws Exception {
        User u = sampleAccessUser();

        // TODO: [by YS] missing - last login, realm, api key, credentials expired

        //TODO: [by YS] where? genPasswordKey, getSalt
        UserInfo i = UserMapper.toArtifactoryUser(u);

        assertEquals(i.getUsername(), u.getUsername());
        assertEquals(i.getEmail(), u.getEmail());
        assertEquals(i.isAccountNonLocked(), true); // ?
        assertEquals(i.isAnonymous(), false);
        assertEquals(i.isAdmin(), true);
        assertEquals(i.isGroupAdmin(), false);
        assertEquals(i.isCredentialsExpired(), false);
        assertEquals(i.isEnabled(), true);
        assertEquals(i.isLocked(), false);
        assertEquals(i.isTransientUser(), false);
        assertEquals(i.isUpdatableProfile(), false);
        //assertEquals(i.isExternal(), true);   // realm
        //assertEquals(i.getLastLoginTimeMillis(), u.getLastLogin());
        //assertEquals(i.getLastLoginClientIp(), u.getLastLoginIp());
        //assertEquals(i.getRealm(), u.getRealm());
        assertEquals(i.getGroups(), Sets.newHashSet(InfoFactoryHolder.get().createUserGroup("northmen", "internal"),
                InfoFactoryHolder.get().createUserGroup("kings", "internal")));
        assertEquals(i.getBintrayAuth(), u.getCustomData("bintray_auth"));
        assertEquals(i.getPrivateKey(), u.getCustomData("private_key"));
        assertEquals(i.getPublicKey(), u.getCustomData("public_key"));
        assertThat(i.getUserProperties()).hasSize(3).containsOnly(
                new UserProperty("charismatic", "true"),
                new UserProperty("bold", "true"),
                new UserProperty("height", "210"));
    }

    public void fullGroupAdminToUserInfo() {
        UserWithGroups u = sampleAccessGroupAdminUser();
        UserInfo i = UserMapper.toArtifactoryUser(u);
        assertEquals(i.getUsername(), u.getUsername());
        assertEquals(i.getEmail(), u.getEmail());
        assertEquals(i.isAccountNonLocked(), true); // ?
        assertEquals(i.isAnonymous(), false);
        assertEquals(i.isAdmin(), false);
        assertEquals(i.isGroupAdmin(), true);
        assertEquals(i.isCredentialsExpired(), false);
        assertEquals(i.isEnabled(), true);
        assertEquals(i.isLocked(), false);
        assertEquals(i.isTransientUser(), false);
        assertEquals(i.isUpdatableProfile(), false);
    }

    public void accessUserToArtifactoryUserWithAllProperties() {
        User u = sampleAccessUser();
        UserInfo userInfo = UserMapper.toArtifactoryUser(u);
        // expect only the 4 custom data properties which aren't mapped to domain fields in UserInfo
        assertThat(userInfo.getUserProperties()).hasSize(3).containsOnly(
                new UserProperty("charismatic", "true"),
                new UserProperty("bold", "true"),
                new UserProperty("height", "210")
        );
    }

    public void fullUserInfoToAccessUser() throws Exception {
        UserInfo i = sampleUserInfo().build();

        // TODO: [by YS] missing - realm, api key

        //TODO: [by YS] where? genPasswordKey, getSalt
        UserRequest u = UserMapper.toAccessUser(i);

        assertEquals(u.getUsername(), i.getUsername());
        assertEquals(u.getPassword(), i.getPassword());
        assertEquals(u.getEmail(), i.getEmail());
        assertEquals(u.getBooleanCustomData("artifactory_admin"), true);
        assertEquals(u.getBooleanCustomData("updatable_profile"), false);
        //assertEquals(i.isExternal(), true);   // realm
        //assertEquals(i.getLastLoginTimeMillis(), u.getLastLoginTime());
        //assertEquals(i.getLastLoginClientIp(), u.getLastLoginIp());
        assertEquals(u.getRealm().getName(), i.getRealm());
        assertEquals(u.getGroups(), Sets.newHashSet("northmen", "kings"));
        assertEquals(u.getCustomData("bintray_auth"), i.getBintrayAuth());
        assertEquals(u.getCustomData("private_key"), i.getPrivateKey());
        assertEquals(u.getCustomData("public_key"), i.getPublicKey());
        assertNull(u.getCustomData("charismatic"));
    }

    public void userInfoToAccessUserWithProperties() {
        UserInfo artiUser = sampleUserInfo().build();
        UserRequest accessUser = UserMapper.toAccessUser(artiUser, false);
        assertNotNull(accessUser);
        // make sure custom data include only the Artifactory built in custom properties
        Map<String, String> customData = accessUser.getCustomData();
        for (String s : customData.keySet()) {
            assertTrue(EnumUtils.isValidEnum(UserMapper.ArtifactoryBuiltInUserProperty.class, s));
        }
    }

    public void userInfoToAccessUserWithNullProperties() {
        UserInfo artiUser = sampleUserInfo().build();
        UserRequest accessUser = UserMapper.toAccessUser(artiUser, true);
        assertNotNull(accessUser);
        assertThat(accessUser.getCustomData()).includes(MapAssert.entry("charismatic", "true"),
                MapAssert.entry("charismatic", "true"), MapAssert.entry("height", "210"));
    }

    public void testNotSavingEmptyCustomProperties() {
        UserInfo artiUser = sampleUserInfo().addProp(new UserProperty("something", "")).bintrayAuth("").build();
        UserRequest accessUser = UserMapper.toAccessUser(artiUser, true);
        assertNotNull(accessUser);
        assertThat(accessUser.getCustomData()).includes(MapAssert.entry("charismatic", "true"),
                MapAssert.entry("charismatic", "true"), MapAssert.entry("height", "210"));
        assertThat(accessUser.getCustomData().get("something")).isNull();
        assertThat(accessUser.getCustomData().get("bintray_auth")).isNull();
    }

    public void testDeleteExistPropertiesOnUpdate() {
        MutableUserInfo artiUser = sampleUserInfo().addProp(new UserProperty("something", "")).bintrayAuth("").build();
        UserRequest accessUser = UserMapper.toUpdateUserRequest(artiUser, true);
        assertNotNull(accessUser);
        Map<String, String> customData = accessUser.getCustomData();
        assertThat(customData.get("something")).isNull();
        assertThat(customData.get("bintray_auth")).isNull();
    }

    private UserResponse sampleAccessUser() {
        return new UserResponse()
                .username("bethod")
                .firstName("Bethod")
                .lastName("The King")
                .email("bethod@northmen.com")
                .status(UserStatus.ENABLED)
                .allowedIp("*")
                .created(fromIsoDateString("1978-05-15T09:15:56.003Z"))
                .modified(fromIsoDateString("1978-05-15T09:15:56.003Z"))
                .lastLoginTime(fromIsoDateString("1980-05-15T09:15:56.003Z"))
                .lastLoginIp("10.0.0.2")
                .groups(Sets.newHashSet("northmen", "kings"))
                .realm(Realm.INTERNAL)
                .addCustomData(gen_password_key.name(), "ZW4gTmluZWZpbmdlcnMTG9n")
                .addCustomData(updatable_profile.name(), "false")
                .addCustomData(bintray_auth.name(), "bethodn")
                .addCustomData(private_key.name(), "TG9nZW4gTmluZWZpbmdlcnM=")
                .addCustomData(public_key.name(), "VGhlIEJsb29keS1OaW5l")
                .addCustomData(artifactory_admin.name(), "true")
                .addCustomData("charismatic", "true")
                .addCustomData("bold", "true")
                .addCustomData("height", "210");
    }

    private UserWithGroupsResponse sampleAccessGroupAdminUser() {
        return new UserWithGroupsResponse()
                .username("bethod")
                .firstName("Bethod")
                .lastName("The King")
                .email("bethod@northmen.com")
                .status(UserStatus.ENABLED)
                .allowedIp("*")
                .created(fromIsoDateString("1978-05-15T09:15:56.003Z"))
                .modified(fromIsoDateString("1978-05-15T09:15:56.003Z"))
                .lastLoginTime(fromIsoDateString("1980-05-15T09:15:56.003Z"))
                .lastLoginIp("10.0.0.2")
                .groups(getAdminUserGroups())
                .realm(Realm.INTERNAL)
                .addCustomData(gen_password_key.name(), "ZW4gTmluZWZpbmdlcnMTG9n")
                .addCustomData(updatable_profile.name(), "false")
                .addCustomData(bintray_auth.name(), "bethodn")
                .addCustomData(private_key.name(), "TG9nZW4gTmluZWZpbmdlcnM=")
                .addCustomData(public_key.name(), "VGhlIEJsb29keS1OaW5l")
                .addCustomData("charismatic", "true")
                .addCustomData("bold", "true")
                .addCustomData("height", "210");
    }

    private ArrayList<GroupResponse> getAdminUserGroups() {
        Map<String, String> customData = Maps.newHashMap();
        customData.put(artifactory_admin.name(), "true");
        GroupResponse group1 = new GroupResponse().name("group1").realm(Realm.INTERNAL).customData(customData);
        return Lists.newArrayList(group1);
    }

    private static long fromIsoDateString(String dateTime) {
        return ISODateTimeFormat.dateTime().parseMillis(dateTime);
    }

    private UserInfoBuilder sampleUserInfo() {
        return new UserInfoBuilder("bethod")
                .password(new SaltedPassword("calder", "scale"))
                .email("bethod@northmen.com")
                .enabled(true)
                //.created("1978-05-15T09:15:56.003Z")
                //.lastModified("1978-05-15T09:15:56.003Z")
                .groups(Sets.newHashSet(InfoFactoryHolder.get().createUserGroup("northmen", "internal"),
                        InfoFactoryHolder.get().createUserGroup("kings", "internal")))
                .credentialsExpired(false)
                .updatableProfile(false)
                .bintrayAuth("bethodn")
                .privateKey("TG9nZW4gTmluZWZpbmdlcnM=")
                .publicKey("VGhlIEJsb29keS1OaW5l")
                .admin(true)
                .realm("internal")
                .lastLogin(fromIsoDateString("1980-05-15T09:15:56.003Z"), "10.0.0.2")
                .addProp(new UserProperty("charismatic", "true"))
                .addProp(new UserProperty("bold", "true"))
                .addProp(new UserProperty("height", "210"));
    }

}
package org.artifactory.security.access;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.accesstoken.AccessClientSettings;
import org.artifactory.test.TestUtils;
import org.jfrog.access.client.token.TokenRequest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.artifactory.descriptor.security.accesstoken.AccessClientSettings.USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Yinon Avraham
 */
public class AccessServiceImplTest {

    @Test(dataProvider = "provideNormalizeInstanceId")
    public void testNormalizeInstanceId(String original, String expected) throws Exception {
        assertEquals(AccessServiceImpl.normalizeInstanceId(original), expected);
    }

    @DataProvider
    private static Object[][] provideNormalizeInstanceId() {
        String uuid = UUID.randomUUID().toString();
        return new Object[][]{
                { uuid, uuid },
                { "b0902ad17dfa28f5:7c169c4a:15849ae7faa:-8000", "b0902ad17dfa28f5_7c169c4a_15849ae7faa_-8000" },
                { " asdf 1234-erty_dfg5+sfg:sdf65=ghtr54 ", "_asdf_1234-erty_dfg5_sfg_sdf65_ghtr54" }
        };
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testNormalizeInstanceIdTooShort() throws Exception {
        AccessServiceImpl.normalizeInstanceId("foo");
    }

    @Test(dataProvider = "provideValidExpiresInForNonAdmin")
    public void testAssertExpiresInForNonAdminUsers(boolean nonnull, Long maxExpiresIn, Long expiresIn) throws Exception {
        AccessServiceImpl accessService = new AccessServiceImpl();
        CentralConfigService centralConfigService = createMock(InternalCentralConfigService.class);
        CentralConfigDescriptorImpl centralConfig = new CentralConfigDescriptorImpl();
        centralConfig.setSecurity(new SecurityDescriptor());
        AccessClientSettings accessClientSettings = null;
        if (nonnull) {
            accessClientSettings = new AccessClientSettings();
            accessClientSettings.setUserTokenMaxExpiresInMinutes(maxExpiresIn);
        }
        centralConfig.getSecurity().setAccessClientSettings(accessClientSettings);
        expect(centralConfigService.getDescriptor()).andReturn(centralConfig).anyTimes();
        replay(centralConfigService);
        TestUtils.setField(accessService, "centralConfigService", centralConfigService);

        TokenRequest tokenRequest = TokenRequest.scopes("api:*").nonRefreshable().expiresIn(expiresIn).build();
        accessService.assertValidExpiresInForNonAdmin(tokenRequest, "testuser");
    }

    @DataProvider
    public static Object[][] provideValidExpiresInForNonAdmin() {
        long validDefaultExpiresIn = USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT * 60 / 2;
        return new Object[][]{
                { false, null, validDefaultExpiresIn }, //expects default max expires in
                { true, null, validDefaultExpiresIn }, //expects default max expires in
                { true, null, USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT }, //expects default max expires in
                { true, 10L, 5L * 60 },
                { true, 10L, 10L * 60 },
                { true, 0L, 10L },
                { true, 0L, null },
                { true, 0L, Long.MAX_VALUE },
        };
    }

    @Test(dataProvider = "provideInvalidExpiresInForNonAdmin", expectedExceptions = AuthorizationException.class)
    public void failAssertExpiresInForNonAdminUsers(boolean nonnull, Long maxExpiresIn, Long expiresIn) throws Exception {
        testAssertExpiresInForNonAdminUsers(nonnull, maxExpiresIn, expiresIn);
    }

    @DataProvider
    public static Object[][] provideInvalidExpiresInForNonAdmin() {
        long invalidDefaultExpiresIn = USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT * 60 + 1;
        return new Object[][]{
                { false, null, invalidDefaultExpiresIn }, //expects default max expires in
                { true, null, invalidDefaultExpiresIn }, //expects default max expires in
                { true, 10L, 10L * 60 + 1 },
                { true, 10L, null }
        };
    }

    @Test
    public void testEncryptOrDecrypt() throws Exception {
        AccessServiceImpl service = new AccessServiceImpl();
        ArtifactoryAccessClientConfigStore configStore = createMock(ArtifactoryAccessClientConfigStore.class);
        TestUtils.setField(service, "configStore", configStore);
        //encrypt
        configStore.encryptOrDecryptAccessCreds(true);
        expectLastCall().once();
        replay(configStore);
        service.encryptOrDecrypt(true);
        verify(configStore);
        //decrypt
        reset(configStore);
        configStore.encryptOrDecryptAccessCreds(false);
        expectLastCall().once();
        replay(configStore);
        service.encryptOrDecrypt(false);
        verify(configStore);
    }
}
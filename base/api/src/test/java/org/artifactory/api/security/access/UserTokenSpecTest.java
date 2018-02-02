package org.artifactory.api.security.access;

import org.jfrog.access.common.ServiceId;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * Created by Yinon Avraham.
 */
public class UserTokenSpecTest {

    @Test
    public void testCreate() throws Exception {
        UserTokenSpec tokenSpec = UserTokenSpec.create("theuser")
                .scope(singletonList("scope"))
                .audience(singletonList("audience"))
                .expiresIn(300L)
                .refreshable(true);
        assertEquals(tokenSpec.getUsername(), "theuser");
        assertEquals(tokenSpec.getScope(), singletonList("scope"));
        assertEquals(tokenSpec.getAudience(), singletonList("audience"));
        assertEquals(tokenSpec.getExpiresIn(), Long.valueOf(300L));
        assertEquals(tokenSpec.getRefreshable(), Boolean.TRUE);
        assertEquals(tokenSpec.isRefreshable(), true);
        ServiceId serviceId = new ServiceId("type", "123");
        assertEquals(tokenSpec.createSubject(serviceId).getFormattedName(), serviceId + "/users/theuser");
    }

    @Test
    public void testCreateWithDefaults() throws Exception {
        UserTokenSpec tokenSpec = UserTokenSpec.create(null);
        assertEquals(tokenSpec.getUsername(), null);
        assertEquals(tokenSpec.getScope(), emptyList());
        assertEquals(tokenSpec.getAudience(), emptyList());
        assertEquals(tokenSpec.getExpiresIn(), null);
        assertEquals(tokenSpec.getRefreshable(), null);
        assertEquals(tokenSpec.isRefreshable(), false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "provideIllegalUsernames")
    public void failCreateSubjectWithIllegalUsername(String illegalUsername) throws Exception {
        UserTokenSpec tokenSpec = UserTokenSpec.create(illegalUsername);
        tokenSpec.createSubject(new ServiceId("type", "123"));
    }

    @DataProvider
    public static Object[][] provideIllegalUsernames() {
        return new Object[][]{
                { null },
                { "" },
                { "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwx" } //60 characters (see: RTFACT-14505)
        };
    }

    @Test(dataProvider = "provideIsUserToken")
    public void testIsUserToken(String subject, boolean expected) throws Exception {
        TokenInfo tokenInfo = createTokenInfo(subject);
        assertEquals(UserTokenSpec.isUserToken(tokenInfo), expected);
    }

    @DataProvider
    public static Object[][] provideIsUserToken() {
        return new Object[][] {
                { "", false },
                { null, false },
                { "service@id", false },
                { "service@id/users", false },
                { "service@id/nodes/node-id", false },
                { "service@id/users/the-user/foo", false },
                { "service@id/users/the-user", true }
        };
    }

    private static TokenInfo createTokenInfo(String subject) {
        TokenInfo tokenInfo = createMock(TokenInfo.class);
        expect(tokenInfo.getSubject()).andReturn(subject).anyTimes();
        replay(tokenInfo);
        return tokenInfo;
    }
}
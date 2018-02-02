package org.artifactory.security.access;

import org.jfrog.access.client.AccessClient;
import org.jfrog.access.client.AccessClientHttpException;
import org.jfrog.access.client.auth.AuthClient;
import org.jfrog.access.model.MessageModelCode;
import org.jfrog.access.rest.user.LoginRequest;
import org.jfrog.access.rest.user.UserWithGroupsResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.testng.annotations.*;

import java.util.HashMap;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Noam Shemesh
 */
public class AccessUserPassAuthenticationProviderTest {

    private AccessUserPassAuthenticationProvider accessUserPassAuthenticationProvider;
    private AccessClient accessClient;
    private AuthClient authClient;
    private AccessService accessService;
    private UsernamePasswordAuthenticationToken defaultAuth;

    @BeforeClass
    public void before() throws Exception {
        authClient = createMock(AuthClient.class);
        accessClient = createMock(AccessClient.class);
        accessService = createStrictMock(AccessService.class);

        expect(accessClient.auth()).andReturn(authClient).anyTimes();
        expect(accessService.getAccessClient()).andReturn(accessClient).anyTimes();

        replay(accessClient, accessService);

        accessUserPassAuthenticationProvider = new AccessUserPassAuthenticationProvider(accessService, null);
    }

    @AfterClass
    public void after() throws Exception {
        verify(accessClient, accessService);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        defaultAuth = new UsernamePasswordAuthenticationToken("noam", "psswrd");
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        verify(authClient);
        reset(authClient);
    }

    @Test
    public void testAuthenticateSuccess() throws Exception {
        expect(authClient.login(anyObject(LoginRequest.class))).andReturn(new UserWithGroupsResponse("noam").customData(new HashMap<>())).once();
        replay(authClient);

        Authentication auth = accessUserPassAuthenticationProvider.authenticate(defaultAuth);

        assertEquals(auth.isAuthenticated(), true);
    }

    @Test(expectedExceptions = AuthenticationServiceException.class)
    public void testAuthenticateFailed() throws Exception {
        expect(authClient.login(anyObject(LoginRequest.class))).andThrow(new AccessClientHttpException(401, "not authed")).once();
        replay(authClient);

        accessUserPassAuthenticationProvider.authenticate(defaultAuth);
    }

    @Test(expectedExceptions = LockedException.class)
    public void testAuthenticateForbidden() throws Exception {
        expect(authClient.login(anyObject(LoginRequest.class))).andThrow(new AccessClientHttpException(429, "{ \"errors\": [ { \"code\": \"" + MessageModelCode.CODE_TOO_MANY_REQUESTS + "\" } ] }")).once();
        replay(authClient);

        accessUserPassAuthenticationProvider.authenticate(defaultAuth);
    }

    @Test(expectedExceptions = CredentialsExpiredException.class)
    public void testCredentialsExpired() throws Exception {
        expect(authClient.login(anyObject(LoginRequest.class))).andThrow(new AccessClientHttpException(403,
                "{ \"errors\": [ { \"code\": \"" + MessageModelCode.CODE_PASSWORD_EXPIRED + "\" } ] }")).once();
        replay(authClient);

        accessUserPassAuthenticationProvider.authenticate(defaultAuth);
    }
}
package org.artifactory.ui.rest.service.admin.security.auth.forgotpassword;

import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestResponse;
import org.artifactory.security.MutableUserInfo;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;

/**
 * @author Noam Shemesh
 */
public class ResetPasswordServiceTest {


    public static final String GEN_PASSWORD_KEY = "gen-password-key";
    private ResetPasswordService resetPasswordService;
    private UserGroupService userGroupService;
    private SecurityService securityService;
    private ArtifactoryRestResponse restResponse;
    private MutableUserInfo user;

    @BeforeMethod
    public void before() {
        userGroupService = createMock(UserGroupService.class);
        securityService = createMock(SecurityService.class);
        restResponse = createMock(ArtifactoryRestResponse.class);
        user = createMock(MutableUserInfo.class);
        expect(user.getUsername()).andReturn("test").once();
        expect(user.getGenPasswordKey()).andReturn(GEN_PASSWORD_KEY).once();

        EasyMock.expect(userGroupService.findUser(EasyMock.anyObject())).andReturn(user).once();

        resetPasswordService = new ResetPasswordService(userGroupService, securityService);
    }

    @AfterMethod
    public void after() {
        verify(userGroupService, securityService);
    }

    @Test
    public void testResetPasswordSuccess() {
        String newPassword = "abc";

        EasyMock.expect(restResponse.info("Password reset successfully.")).andReturn(restResponse).once();
        securityService.changePasswordWithoutValidation(user, newPassword);
        EasyMock.expectLastCall().once();
        replay(userGroupService, securityService, restResponse, user);

        resetPasswordService.saveNewPassword(restResponse, GEN_PASSWORD_KEY, "test", newPassword);
    }

    @Test
    public void testWrongKey() {
        EasyMock.expect(restResponse.error("key is not valid")).andReturn(restResponse);
        replay(userGroupService, securityService, restResponse, user);

        resetPasswordService.saveNewPassword(restResponse, "wrong!", "test", "dfs");
    }

    @Test
    public void testEmptyKey() {
        EasyMock.expect(restResponse.error("key is not valid")).andReturn(restResponse);
        replay(userGroupService, securityService, restResponse, user);

        resetPasswordService.saveNewPassword(restResponse, "", "test", "123");
    }
}
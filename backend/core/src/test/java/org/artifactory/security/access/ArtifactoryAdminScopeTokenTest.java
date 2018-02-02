package org.artifactory.security.access;

import org.jfrog.access.common.ServiceId;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.artifactory.security.access.AccessServiceConstants.ARTIFACTORY_SERVICE_TYPE;
import static org.testng.Assert.assertEquals;

/**
 * @author Yinon Avraham.
 */
public class ArtifactoryAdminScopeTokenTest {

    @Test(dataProvider = "provideParse")
    public void testParse(String scopeToken, ServiceId serviceId) throws Exception {
        ArtifactoryAdminScopeToken adminScopeToken = ArtifactoryAdminScopeToken.parse(scopeToken);
        assertEquals(adminScopeToken.getServiceId(), serviceId);
        assertEquals(adminScopeToken.getScopeToken(), scopeToken);
        assertEquals(adminScopeToken.toString(), scopeToken);
    }

    @DataProvider
    public static Object[][] provideParse() {
        ServiceId artServiceId1 = ServiceId.generateUniqueId(ARTIFACTORY_SERVICE_TYPE);
        ServiceId artServiceId2 = ServiceId.generateUniqueId(ARTIFACTORY_SERVICE_TYPE);
        ServiceId artServiceId3 = ServiceId.generateUniqueId("jf-artifactory");
        return new Object[][]{
                { artServiceId1 + ":admin", artServiceId1 },
                { artServiceId2 + ":admin", artServiceId2 },
                { artServiceId3 + ":admin", artServiceId3 }
        };
    }

    @Test(dataProvider = "provideAccepts")
    public void testAccepts(String scopeToken, boolean expected) throws Exception {
        assertEquals(ArtifactoryAdminScopeToken.accepts(scopeToken), expected);
    }

    @DataProvider
    public static Object[][] provideAccepts() {
        ServiceId artServiceId1 = ServiceId.generateUniqueId(ARTIFACTORY_SERVICE_TYPE);
        return new Object[][]{
                { ServiceId.generateUniqueId("jf-artifactory") + ":admin", true },
                { artServiceId1 + ":admin", true },
                { artServiceId1 + ":admin ", false },
                { " " + artServiceId1 + ":admin", false },
                { artServiceId1 + ":admins", false },
                { artServiceId1 + ":", false },
                { artServiceId1 + "", false },
                { ServiceId.generateUniqueId("other-service-type") + ":admin", false }
        };
    }
}
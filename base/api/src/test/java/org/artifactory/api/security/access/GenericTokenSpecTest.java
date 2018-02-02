package org.artifactory.api.security.access;

import org.jfrog.access.common.ServiceId;
import org.jfrog.access.common.SubjectFQN;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yinon Avraham.
 */
public class GenericTokenSpecTest {

    @Test(dataProvider = "provideAccepts")
    public void testAccepts(String subject, boolean expected) throws Exception {
        assertEquals(GenericTokenSpec.accepts(subject), expected);
    }

    @DataProvider
    public static Object[][] provideAccepts() {
        return new Object[][]{
                { "service-type@123", true },
                { "service-type@123/foo", true },
                { "service-type@123/foo/bar", true },
                { "service-type@123/foo/bar/12345", true },
                { "service-type/foo/bar/12345", false },
                { "@service-type/foo/bar/12345", false },
                { "service-type@/foo/bar/12345", false },
                { "foo", false },
                { "", false },
                { null, false }
        };
    }

    @Test
    public void testCreateSubjectIgnoresProvidedServiceId() throws Exception {
        ServiceId serviceId1 = new ServiceId("service-type", "123");
        ServiceId serviceId2 = new ServiceId("service-type", "234");

        String subject1 = serviceId1.getFormattedName();
        GenericTokenSpec tokenSpec1 = GenericTokenSpec.create(subject1);
        assertEquals(tokenSpec1.createSubject(serviceId1), SubjectFQN.fromFullyQualifiedName(subject1));
        assertEquals(tokenSpec1.createSubject(serviceId2), SubjectFQN.fromFullyQualifiedName(subject1));

        SubjectFQN subject2 = new SubjectFQN(serviceId1, "foo", "bar");
        GenericTokenSpec tokenSpec2 = GenericTokenSpec.create(subject2.getFormattedName());
        assertEquals(tokenSpec2.createSubject(serviceId1), subject2);
        assertEquals(tokenSpec2.createSubject(serviceId2), subject2);
    }
}
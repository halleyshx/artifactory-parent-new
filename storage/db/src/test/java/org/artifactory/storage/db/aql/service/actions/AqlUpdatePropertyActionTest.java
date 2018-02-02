package org.artifactory.storage.db.aql.service.actions;

import org.artifactory.api.properties.PropertiesService;
import org.artifactory.descriptor.property.Property;
import org.artifactory.repo.RepoPath;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @author Dan Feldman
 */
@Test
public class AqlUpdatePropertyActionTest extends AqlAbstractActionTest {

    private PropertiesService propsService = Mockito.mock(PropertiesService.class);

    @BeforeClass
    @Override
    public void springTestContextPrepareTestInstance() throws Exception {
        super.springTestContextPrepareTestInstance();
        addBean(propsService, PropertiesService.class);
    }

    @BeforeClass(dependsOnMethods = "springTestContextPrepareTestInstance")
    public void setup() {
        super.setup();
    }

    @BeforeMethod
    public void cleanUp() {
        reset(propsService);
    }

    @Test
    public void testUpdateStringQuery() throws Exception {
        String result = executeQuery("properties.update({\"build.number\" : {\"$eq\" : \"67\"}})" +
                ".keys(\"build.number\", \"dummy.nonexistent.prop\")" +
                ".newValue(\"4000\")" +
                ".dryRun(\"false\")");

        assertThat(result).contains("\"repo\" : \"repo1\"");
        assertThat(result).contains("\"path\" : \"ant/ant/1.5\"");
        assertThat(result).contains("\"name\" : \"ant-1.5.jar\"");
        assertThat(result).contains("\"total\" : 1");

        // There should be 2 updates: 1 row has build.number = 67 and 2 properties were requested for the change.
        // The second property is dummy - the edit method is called for it but it will not change if it doesn't exist.
        verify(propsService, atLeast(2))
                .editProperty( any(RepoPath.class), eq(null), any(Property.class), eq(true), any(String.class));
        verify(propsService, atMost(2))
                .editProperty( any(RepoPath.class), eq(null), any(Property.class), eq(true), any(String.class));
    }

    // Only works with the result streamer for now
    @Test(enabled = false)
    public void testDeleteApiQuery() {
    }
}

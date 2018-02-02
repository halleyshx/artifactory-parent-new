package org.artifactory.update.security.v8;

import org.artifactory.update.security.SecurityConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Shemesh
 */
public class Md5TemplateHashPasswordConverterTest extends SecurityConverterTest {
    @Test
    public void testConvertWithoutSalt() throws Exception {
        List children = getUsersAfterConvert();

        Element user = (Element) children.get(1);

        assertEquals(user.getChild("username").getText(), "noamt");
        assertEquals(user.getChild("password").getText(), "md5$1$$dc16df3b1c37da44dadb835784812123");
    }

    @Test
    public void testConvertWithSalt() throws Exception {
        List children = getUsersAfterConvert();

        Element user = (Element) children.get(4);

        assertEquals(user.getChild("username").getText(), "yoava");
        assertEquals(user.getChild("password").getText(), "md5$1$CAFEBABEEBABEFAC$be8a7ba7f2c82b712fb4051192612123");
    }

    @Test
    public void testConvertWithoutPassword() throws Exception {
        List children = getUsersAfterConvert();

        Element user = (Element) children.get(0);

        assertEquals(user.getChild("username").getText(), "anonymous");
        assertNull(user.getChild("password"));
    }

    private List getUsersAfterConvert() throws Exception {
        String fileMetadata = "/security/v8/security.xml";
        Document document = convertXml(fileMetadata, new Md5TemplateHashPasswordConverter());
        Element rootElement = document.getRootElement();
        Element child = rootElement.getChild("users");
        return child.getChildren("user");
    }
}
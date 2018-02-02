package org.artifactory.version.converter.v207;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Shemesh
 */
public class RemoveAccessAdminCredentialsConverterTest extends XmlConverterTest {

    private static final String CONFIG_XML_WITH_ACCESS_ADMIN = "/config/test/config.2.0.7.with_adminToken.xml";
    private static final String CONFIG_XML_WITHOUT_ACCESS_ADMIN = "/config/test/config.2.0.7.without_adminToken.xml";

    private final RemoveAccessAdminCredentialsConverter converter = new RemoveAccessAdminCredentialsConverter();

    @Test
    public void convertWithPreviousData() throws Exception {
        Document document = convertXml(CONFIG_XML_WITH_ACCESS_ADMIN, converter);
        validateXml(document);
    }

    @Test
    public void convertWithoutPreviousData() throws Exception {
        Document document = convertXml(CONFIG_XML_WITHOUT_ACCESS_ADMIN, converter);
        validateXml(document);
    }

    private void validateXml(Document document) {
        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        Element accessClientSettings = security.getChild("accessClientSettings", ns);
        assertNotNull(accessClientSettings);
        Element accessToken = accessClientSettings.getChild("adminToken", ns);
        assertNull(accessToken);
        Element expires = accessClientSettings.getChild("userTokenMaxExpiresInMinutes", ns);
        assertNotNull(expires);
    }
}
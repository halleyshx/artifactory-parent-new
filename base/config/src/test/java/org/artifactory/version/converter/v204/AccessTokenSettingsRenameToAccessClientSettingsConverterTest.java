package org.artifactory.version.converter.v204;

import org.artifactory.convert.XmlConverterTest;
import org.artifactory.version.converter.v204.AccessTokenSettingsRenameToAccessClientSettingsConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Yinon Avraham.
 */
@Test
public class AccessTokenSettingsRenameToAccessClientSettingsConverterTest extends XmlConverterTest {

    private static final String CONFIG_XML_WITH_ACCESS_TOKEN_SETTINGS = "/config/test/config.2.0.3.with_accessTokenSettings.xml";
    private static final String CONFIG_XML_WITHOUT_ACCESS_TOKEN_SETTINGS = "/config/test/config.2.0.3.without_accessTokenSettings.xml";

    private final AccessTokenSettingsRenameToAccessClientSettingsConverter converter = new AccessTokenSettingsRenameToAccessClientSettingsConverter();

    public void convertWithPreviousData() throws Exception {
        Document document = convertXml(CONFIG_XML_WITH_ACCESS_TOKEN_SETTINGS, converter);
        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        Element accessTokenSettings = security.getChild("accessTokenSettings", ns);
        assertNull(accessTokenSettings);
        Element accessClientSettings = security.getChild("accessClientSettings", ns);
        assertNotNull(accessClientSettings);
        assertEquals(accessClientSettings.getChild("userTokenMaxExpiresInMinutes", ns).getValue(), "60");
        assertEquals(accessClientSettings.getChildren().size(), 1);
    }

    public void convertWithoutPreviousData() throws Exception {
        Document document = convertXml(CONFIG_XML_WITHOUT_ACCESS_TOKEN_SETTINGS, converter);
        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        Element accessTokenSettings = security.getChild("accessTokenSettings", ns);
        assertNull(accessTokenSettings);
        Element accessClientSettings = security.getChild("accessClientSettings", ns);
        assertNull(accessClientSettings);
    }

}

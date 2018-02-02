/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.version.converter.v177;

import org.artifactory.convert.XmlConverterTest;
import org.artifactory.version.converter.v178.SigningKeysConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;


import static org.testng.Assert.*;

/**
 * @author Uriah Levy
 */
public class SigningKeysConverterTest extends XmlConverterTest {
    private String CONFIG_XML = "/config/test/config.1.7.7_signing_settings.xml";

    @Test
    public void convert() throws Exception {
        Document document = convertXml(CONFIG_XML, new SigningKeysConverter());
        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        validateConfig(security.getChild("signingKeysSettings", ns), ns);
    }

    private void validateConfig(Element signingKeysSettings, Namespace namespace) {
        // ensure debianSettings is re-named to signingKeysSettings
        assertTrue(signingKeysSettings.getName().equals("signingKeysSettings"));
    }

}

/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.version.converter.v178;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Uriah Levy
 */
public class SigningKeysConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(SigningKeysConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting Debian Keys -> Signing Keys conversion");
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        if (security == null) {
            log.debug("no security settings to convert");
            return;
        }
        convertConfig(security.getChild("debianSettings", ns));
        log.info("Finished Debian Keys -> Signing Keys conversion");
    }

    private void convertConfig(Element debianSettings) {
        if (debianSettings != null) {
            log.debug("Changing debianSettings to signingKeysSettings");
            debianSettings.setName("signingKeysSettings");
        }

    }
}

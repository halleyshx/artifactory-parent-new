package org.artifactory.version.converter.v177;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author gidis
 */
public class LdapPoisoningProtectionConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(LdapPoisoningProtectionConverter.class);

    @Override
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        if (security == null) {
            log.debug("no security settings");
            return;
        }

        Element ldapSettings = security.getChild("ldapSettings", ns);
        if (ldapSettings == null) {
            log.debug("no ldap settings");
            return;
        }

        List ldapSettingList = ldapSettings.getChildren("ldapSetting", ns);
        if (ldapSettingList == null) {
            log.debug("no ldap settings");
            return;
        }
        for (Object ldapSettingObject : ldapSettingList) {
            Element ldapSetting = (Element) ldapSettingObject;
            Element objectInjectionProtection = ldapSetting.getChild("ldapPoisoningProtection", ns);
            if (objectInjectionProtection == null) {
                log.debug("ldap object injection protection");
                objectInjectionProtection = new Element("ldapPoisoningProtection", ns);
                objectInjectionProtection.setText("true");
                ldapSetting.addContent(objectInjectionProtection);
            }
        }
    }
}

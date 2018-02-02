package org.artifactory.update.security.v8;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jfrog.access.user.HashEncoderUtils;

import java.util.List;

/**
 * @author Noam Shemesh
 */
public class Md5TemplateHashPasswordConverter implements XmlConverter {

    @Override
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();
        Element child = root.getChild("users", namespace);
        List users = child.getChildren("user", namespace);
        if (users != null && !users.isEmpty()) {
            for (Object user : users) {
                Element userElement = (Element) user;
                Element password = userElement.getChild("password", namespace);
                if (password != null) {
                    Element salt = userElement.getChild("salt", namespace);

                    String passwordTemplate = convertPasswordHash(password.getText(), salt != null ? salt.getText() : null);
                    if (salt != null) {
                        userElement.removeChild("salt", namespace);
                    }

                    password.setText(passwordTemplate);
                }
            }
        }
    }

    public static String convertPasswordHash(String password, String salt) {
        return HashEncoderUtils.md5(password, salt == null ? "" : salt, 1);
    }
}
